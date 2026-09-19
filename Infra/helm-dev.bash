SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "SCRIPT_DIR:   $SCRIPT_DIR"
echo "PROJECT_ROOT: $PROJECT_ROOT"
echo "PWD:          $(pwd)"

kubectl create namespace bank-dev --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic jwt-keys \
  --from-file=private.pem="$PROJECT_ROOT/secrets/private.pem" \
  --from-file=public.pem="$PROJECT_ROOT/secrets/public.pem" \
  --namespace bank-dev \
  --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install bank-dev "$PROJECT_ROOT/Infra/helm/dev" \
  -f "$PROJECT_ROOT/Infra/helm/dev/values.yaml" \
  -f "$PROJECT_ROOT/Infra/helm/dev/values-secret.yaml" \
  --namespace bank-dev \
  --create-namespace

kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80