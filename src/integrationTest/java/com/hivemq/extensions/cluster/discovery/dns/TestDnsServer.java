/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.hivemq.extensions.cluster.discovery.dns;

import org.apache.directory.server.dns.DnsServer;
import org.apache.directory.server.dns.io.decoder.DnsMessageDecoder;
import org.apache.directory.server.dns.io.encoder.DnsMessageEncoder;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordImpl;
import org.apache.directory.server.dns.protocol.DnsProtocolHandler;
import org.apache.directory.server.dns.protocol.DnsUdpEncoder;
import org.apache.directory.server.dns.store.DnsAttribute;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.transport.socket.DatagramAcceptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * This Class was inspired by the Netty Project:
 *
 * @author Lukas Brand
 * @see <a
 *         href="https://github.com/netty/netty/blob/4.1/resolver-dns/src/test/java/io/netty/resolver/dns/TestDnsServer.java">TestDnsServer.java</a>
 */
class TestDnsServer extends DnsServer {

    private final @NotNull RecordStore store;

    TestDnsServer(final @NotNull Set<String> domains, final int numOfRecords) {
        this(new FixedRecordStore(domains, numOfRecords));
    }

    TestDnsServer(final @NotNull RecordStore store) {
        this.store = store;
    }

    @Override
    public void start() throws IOException {
        final InetSocketAddress address = new InetSocketAddress("0.0.0.0", 0);
        final UdpTransport transport = new UdpTransport(address.getHostName(), address.getPort());
        setTransports(transport);

        final DatagramAcceptor acceptor = transport.getAcceptor();

        acceptor.setHandler(new DnsProtocolHandler(this, store) {
            @Override
            public void sessionCreated(final @NotNull IoSession session) {
                session.getFilterChain()
                        .addFirst("codec", new ProtocolCodecFilter(new TestDnsProtocolUdpCodecFactory()));
            }
        });

        acceptor.getSessionConfig().setReuseAddress(true);

        // Start the listener
        acceptor.bind();
    }

    public @NotNull InetSocketAddress localAddress() {
        return (InetSocketAddress) getTransports()[0].getAcceptor().getLocalAddress();
    }

    private static final class TestDnsProtocolUdpCodecFactory implements ProtocolCodecFactory {

        @Override
        public ProtocolEncoder getEncoder(final @NotNull IoSession session) {
            return new DnsUdpEncoder() {
                private final @NotNull DnsMessageEncoder encoder = new DnsMessageEncoder();

                @Override
                public void encode(
                        final @NotNull IoSession session,
                        final @NotNull Object message,
                        final @NotNull ProtocolEncoderOutput out) {

                    final IoBuffer buf = IoBuffer.allocate(1024);
                    final DnsMessage dnsMessage = (DnsMessage) message;
                    encoder.encode(buf, dnsMessage);
                    buf.flip();
                    out.write(buf);
                }
            };
        }

        @Override
        public ProtocolDecoder getDecoder(final @NotNull IoSession session) {
            return new ProtocolDecoderAdapter() {
                private final @NotNull DnsMessageDecoder decoder = new DnsMessageDecoder();

                @Override
                public void decode(
                        final @NotNull IoSession session,
                        final @NotNull IoBuffer in,
                        final @NotNull ProtocolDecoderOutput out) throws IOException {

                    final DnsMessage message = decoder.decode(in);
                    out.write(message);
                }
            };
        }
    }

    private static final class FixedRecordStore implements RecordStore {

        private final @NotNull Set<String> domains;
        private final int numOfRecords;

        FixedRecordStore(final @NotNull Set<String> domains, final int numOfRecords) {
            this.domains = domains;
            this.numOfRecords = numOfRecords;
        }

        @Override
        public @Nullable Set<ResourceRecord> getRecords(final @NotNull QuestionRecord questionRecord) {
            final String name = questionRecord.getDomainName();
            if (domains.contains(name) && (questionRecord.getRecordType() == RecordType.A)) {
                final Set<ResourceRecord> records = new HashSet<>();
                for (int i = 0; i < numOfRecords; i++) {
                    records.add(newARecord(name, i + ".2.3.4"));
                }
                return records;
            }
            return null;
        }
    }

    private static @NotNull ResourceRecord newARecord(final @NotNull String name, final @NotNull String ipAddress) {
        return new TestResourceRecord(name,
                RecordType.A,
                Map.of(DnsAttribute.IP_ADDRESS.toLowerCase(Locale.US), ipAddress));
    }

    private static final class TestResourceRecord extends ResourceRecordImpl {

        TestResourceRecord(
                final @NotNull String domainName,
                final @NotNull RecordType recordType,
                final @NotNull Map<String, Object> attributes) {

            super(domainName, recordType, RecordClass.IN, 100, attributes);
        }

        //To prevent duplicates of resource records in the set it is used in.
        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            return o == this;
        }
    }
}
