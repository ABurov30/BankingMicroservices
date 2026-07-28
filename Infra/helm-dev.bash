kubectl create namespace bank-dev --dry-run=client -o yaml | kubectl apply -f -

kubectl create secret generic jwt-keys \
  --from-file=private.pem=./Infra/secrets/private.pem \
  --from-file=public.pem=./Infra/secrets/public.pem \
  --namespace bank-dev \
  --dry-run=client -o yaml | kubectl apply -f -

helm upgrade --install bank-dev ./Infra/helm/dev \
  -f ./Infra/helm/dev/values.yaml \
  -f ./Infra/helm/dev/values-secret.yaml \
  --namespace bank-dev \
  --create-namespace
