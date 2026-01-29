terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.10"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

# Cloud Storage
resource "google_storage_bucket" "pdf_bucket" {
  name          = "${var.project_id}-pdf-bucket"
  location      = var.region
  force_destroy = true
}

# Pub/Sub
resource "google_pubsub_topic" "pdf_topic" {
  name = "pdf-merge-topic"
}

resource "google_pubsub_subscription" "pdf_subscription" {
  name  = "pdf-merge-sub"
  topic = google_pubsub_topic.pdf_topic.name
}
# GCE VM
resource "google_compute_instance" "pdf_worker_vm" {
  name         = "pdf-worker-vm"
  machine_type = "e2-small"
  zone         = var.zone

  boot_disk {
    initialize_params {
      image = "projects/cos-cloud/global/images/family/cos-stable"
    }
  }

  network_interface {
    network = "default"
    access_config {}
  }

  metadata = {
    google-logging-enabled = "true"
    gce-container-declaration = <<-EOT
      spec:
        containers:
          - name: pdf-worker
            image: europe-central2-docker.pkg.dev/${var.project_id}/pdf-merger/pdf-worker:latest
            env:
              - name: GCP_PROJECT
                value: ${var.project_id}
              - name: STORAGE_BUCKET
                value: "${var.project_id}-pdf-bucket"
              - name: PUBSUB_SUBSCRIPTION
                value: ${var.pubsub_sub_name}
        restartPolicy: Always
    EOT
  }

  allow_stopping_for_update = true

  service_account {
    email  = "default"
    scopes = ["cloud-platform"]
  }

  tags = ["pdf-worker"]
}

# Cloud Function
resource "google_storage_bucket_object" "function_archive" {
  name   = "function-source.zip"
  bucket = google_storage_bucket.pdf_bucket.name
  source = "../cloud-function/target/function-source.zip"
}

resource "google_cloudfunctions_function" "pdf_function" {
  name        = "pdfMergePublisher"
  runtime     = "java17"
  region      = var.region
  entry_point = "com.cloudproject.function.PdfMergeFunction"

  source_archive_bucket = google_storage_bucket.pdf_bucket.name
  source_archive_object = google_storage_bucket_object.function_archive.name

  trigger_http        = true
  available_memory_mb = 512
  ingress_settings    = "ALLOW_ALL"

    environment_variables = {
        PROJECT_ID    = var.project_id
        TOPIC_NAME    = google_pubsub_topic.pdf_topic.name
        BUCKET_NAME   = google_storage_bucket.pdf_bucket.name
    }
}

# IAM
resource "google_project_iam_member" "vm_pubsub" {
  role   = "roles/pubsub.subscriber"
  member = "serviceAccount:${google_compute_instance.pdf_worker_vm.service_account.0.email}"
  project = var.project_id
}

resource "google_project_iam_member" "vm_storage" {
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_compute_instance.pdf_worker_vm.service_account.0.email}"
  project = var.project_id
}

resource "google_project_iam_member" "vm_monitoring" {
  role   = "roles/monitoring.metricWriter"
  member = "serviceAccount:${google_compute_instance.pdf_worker_vm.service_account.0.email}"
  project = var.project_id
}

resource "google_project_iam_member" "vm_logging" {
  role   = "roles/logging.logWriter"
  member = "serviceAccount:${google_compute_instance.pdf_worker_vm.service_account.0.email}"
  project = var.project_id
}
