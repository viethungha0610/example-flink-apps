resource "aws_s3_bucket" "lakehouse_raw_bucket" {
  bucket = "lakehouse-raw-bucket"
}

resource "aws_s3_bucket" "lakehouse_agg_bucket" {
  bucket = "lakehouse-agg-bucket"
}
