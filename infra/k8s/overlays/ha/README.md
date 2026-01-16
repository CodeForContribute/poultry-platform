# PostgreSQL HA Overlay

This overlay replaces the single-node Postgres StatefulSet with the Patroni-based HA cluster
defined in `infra/k8s/postgres-ha-statefulset.yaml` and adds the read-only service.

Notes:
- Update `postgres-ha-statefulset.yaml` secrets placeholders before applying.
- Ensure `postgres-ha-secrets` credentials match `poultry-secrets` DB credentials.
- Provide an etcd cluster (or update Patroni config to use Kubernetes endpoints).

Apply:
  kustomize build infra/k8s/overlays/ha | kubectl apply -f -
