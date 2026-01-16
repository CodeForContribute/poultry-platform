# Monitoring Overlay

This overlay installs Prometheus and Grafana from `infra/k8s/monitoring/`.

Notes:
- Replace placeholder passwords in `infra/k8s/monitoring/grafana-deployment.yaml`.
- Ensure network access to `prometheus:9090` and `grafana:3000` services.

Apply:
  kustomize build infra/k8s/overlays/monitoring | kubectl apply -f -
