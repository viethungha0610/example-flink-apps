provider "aws" {
  access_key                  = "localstack"                # Dummy access key
  secret_key                  = "localstack"                # Dummy secret key
  region                      = "eu-west-1"           # LocalStack default region

  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3 = "http://s3.localhost.localstack.cloud:4566"
  }
}