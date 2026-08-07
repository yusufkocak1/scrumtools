pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        COMPOSE_PROJECT_NAME = 'scrumtools'
        // Klasik builder her RUN adimi icin ara container acar ve basarisiz olan
        // adimin container'ini geride birakir (rastgele isimli exited 1/2 yiginlari).
        // BuildKit ara container hic olusturmaz; sorunu kaynaginda keser.
        DOCKER_BUILDKIT = '1'
        COMPOSE_DOCKER_CLI_BUILD = '1'
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
    }

    post {
        // Temizlik post.always'te: artik container'lar tam da build BASARISIZ
        // oldugunda kaliyor, o durumda bir 'Cleanup' stage'i hic calismazdi.
        always {
            // .env'i workspace'te birakma
            sh 'rm -f .env'

            // BuildKit devre disi kalirsa (eski daemon, DOCKER_BUILDKIT=0 override)
            // basarisiz RUN adimlari geride rastgele isimli exited container birakir.
            // Servis container'larina dokunmamak icin isim koruma listesi uygulanir;
            // ara build container'larinin adi her zaman rastgele iki kelimedir.
            sh '''
                docker ps -a --filter status=exited --filter status=dead --format '{{.Names}}' |
                  grep -Ev '^(scrumtools-|postgres|minio|mail|nginx-proxy-manager|npm[-_]|portainer|jenkins)' |
                  xargs -r docker rm >/dev/null || true
            '''

            // Container'lar gittikten sonra katmanlari da birak
            sh 'docker image prune -f'
        }
    }
}
