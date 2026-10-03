# infra/main.tf
# A small, intentionally HARDENED Terraform configuration — an S3 bucket for
# build artifacts — used to demonstrate Infrastructure-as-Code security scanning
# (Checkov / tfsec) in the pipeline. It is scanned, not applied, in CI.

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = "ap-south-1"
}

resource "aws_s3_bucket" "artifacts" {
  bucket = "devsecops-artifacts-change-me"
  tags = {
    Project = "devsecops-pipeline"
  }
}

# Encrypt objects at rest.
resource "aws_s3_bucket_server_side_encryption_configuration" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Keep historical versions (and protect against accidental overwrite/delete).
resource "aws_s3_bucket_versioning" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Block all public access.
resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket                  = aws_s3_bucket.artifacts.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}
