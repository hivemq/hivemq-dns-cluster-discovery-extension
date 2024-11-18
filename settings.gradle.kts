rootProject.name = "hivemq-dns-cluster-discovery"

if (file("../hivemq-prometheus-extension").exists()) {
    includeBuild("../hivemq-prometheus-extension")
}
