  helm upgrade --install bank-dev ./Infra/helm/dev \
    -f ./Infra/helm/dev/values.yaml \
    -f ./Infra/helm/dev/values-secret.yaml \
    --namespace bank-dev \
    --create-namespace