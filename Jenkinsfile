pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        COMPOSE_PROJECT_NAME = 'scrumtools'
    }

    stages {
        stage('Prepare env') {
            steps {
                // Jenkins > Credentials > "scrumtools-env" (Secret file) olarak
                // sunucudaki .env icerigini ekleyin. Repo'da .env yoktur (gitignore).
                withCredentials([file(credentialsId: 'scrumtools-env', variable: 'ENV_FILE')]) {
                    sh 'cp "$ENV_FILE" .env'
                }
            }
        }

        stage('Ensure proxy network') {
            steps {
                // nginx-proxy-manager'in kullandigi harici network yoksa olustur
                sh 'docker network inspect proxy >/dev/null 2>&1 || docker network create proxy'
            }
        }

        stage('Build') {
            steps {
                sh 'docker compose build --pull'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker compose up -d --remove-orphans'
            }
        }

        stage('Health check') {
            steps {
                // Backend'in ayaga kalkmasini bekle (frontend nginx uzerinden)
                sh '''
                    for i in $(seq 1 30); do
                        # 401/403 gibi 4xx cevaplar da "ayakta" demektir; HTTP cevabi yeterli
                        if docker compose exec -T frontend sh -c 'wget -S -q -O /dev/null http://backend:8080/api/auth/me 2>&1 | grep -q "HTTP/"'; then
                            echo "Backend ayakta."
                            exit 0
                        fi
                        echo "Bekleniyor... ($i/30)"
                        sleep 5
                    done
                    echo "Backend 150 saniyede ayaga kalkmadi!" >&2
                    docker compose logs --tail=100 backend >&2
                    exit 1
                '''
            }
        }

        stage('Cleanup') {
            steps {
                sh 'docker image prune -f'
            }
        }
    }

    post {
        always {
            // .env'i workspace'te birakma
            sh 'rm -f .env'
        }
    }
}
