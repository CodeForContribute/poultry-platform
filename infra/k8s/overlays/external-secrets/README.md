# External Secrets Overlay

This overlay installs External Secrets Operator and replaces `secrets.yaml` with
ExternalSecret resources under `infra/k8s/external-secrets/`.

Notes:
- Configure the correct SecretStore (AWS/GCP/Azure) before applying.
- See `infra/k8s/external-secrets/aws-secrets-manager-setup.yaml` for AWS IRSA guidance.

Apply:
  kustomize build infra/k8s/overlays/external-secrets | kubectl apply -f -
