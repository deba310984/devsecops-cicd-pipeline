// DevSecOps pipeline (Jenkins declarative) mirroring the GitHub Actions workflow.
// Build & test and secret scanning are gates; other scans are report-only and
// publish their reports. Requires a Docker-capable agent with Trivy, Semgrep,
// Checkov, gitleaks, and Syft available (or run them via their containers).

pipeline {
    agent any

    tools {
        jdk   'jdk21'
        maven 'maven3'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    environment {
        IMAGE = "devsecops-app:${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build & Test') {               // gate
            steps { sh 'mvn -B clean verify' }
            post {
                always { junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true }
                success { archiveArtifacts artifacts: 'target/*.jar', fingerprint: true }
            }
        }

        stage('Secret Scan') {                 // gate
            steps {
                sh 'docker run --rm -v "$PWD:/repo" ghcr.io/gitleaks/gitleaks:latest dir /repo --redact --verbose'
            }
        }

        stage('Security Scans') {              // report-only, run in parallel
            parallel {
                stage('SAST (Semgrep)') {
                    steps { sh 'semgrep scan --config p/java --config p/secrets --sarif --output semgrep.sarif || true' }
                    post { always { archiveArtifacts artifacts: 'semgrep.sarif', allowEmptyArchive: true } }
                }
                stage('Dependencies (Trivy fs)') {
                    steps { sh 'trivy fs --scanners vuln,secret,misconfig --severity HIGH,CRITICAL --exit-code 0 --format table --output trivy-fs.txt .' }
                    post { always { archiveArtifacts artifacts: 'trivy-fs.txt', allowEmptyArchive: true } }
                }
                stage('IaC (Checkov)') {
                    steps { sh 'checkov -d infra --soft-fail --compact' }
                }
            }
        }

        stage('Build Image') {
            steps { sh 'docker build -t $IMAGE .' }
        }

        stage('Image Scan + SBOM') {           // report-only
            steps {
                sh 'trivy image --severity HIGH,CRITICAL --ignore-unfixed --exit-code 0 --format table --output trivy-image.txt $IMAGE'
                sh 'syft $IMAGE -o spdx-json=sbom.spdx.json'
            }
            post { always { archiveArtifacts artifacts: 'trivy-image.txt,sbom.spdx.json', allowEmptyArchive: true } }
        }

        stage('Deploy') {
            steps {
                sh '''
                    docker rm -f devsecops-app || true
                    docker run -d --name devsecops-app -p 8080:8080 $IMAGE
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh '''
                    for i in $(seq 1 15); do
                        curl -fsS http://localhost:8080/health && exit 0
                        echo "waiting... ($i)"; sleep 3
                    done
                    echo "app did not become healthy"; exit 1
                '''
            }
        }
    }

    post {
        always { sh 'docker image prune -f || true' }
    }
}
