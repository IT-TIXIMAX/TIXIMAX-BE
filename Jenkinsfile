pipeline {
    agent any

    environment {
        APP_NAME = "tiximax-be-old"
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Initialize') {
            steps {
                script {

                    def BR = (env.BRANCH_NAME)

                    echo "[Init] Detected branch: ${BR}"

                    def branchMap = [
                        "staging": [
                            envName: "staging",
                            credId : "env-tiximax-be-old-staging",
                        ],

                        "product": [
                            envName: "product",
                            credId : "env-tiximax-be-old-product",
                        ]
                    ]


                    env.ENVIRONMENT_NAME = branchMap[BR].envName
                    env.ENV_CRED_ID      = branchMap[BR].credId
                    env.IMAGE_TAG        = "${env.APP_NAME}:${env.ENVIRONMENT_NAME}-${env.BUILD_NUMBER}"
                    env.APP_NAME_UNIQUE  = "${env.APP_NAME}-${env.ENVIRONMENT_NAME}"

                    echo "[Init] ENVIRONMENT_NAME = ${env.ENVIRONMENT_NAME}"
                    echo "[Init] IMAGE_TAG        = ${env.IMAGE_TAG}"
                    echo "[Init] ENV_CRED_ID      = ${env.ENV_CRED_ID}"
                }
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
                echo "[${env.ENVIRONMENT_NAME}] Code checked out."
            }
        }

        stage('Build') {
            steps {
                echo "[${env.ENVIRONMENT_NAME}] Building Docker image…"

                sh """
                    docker build -t ${env.IMAGE_TAG} .
                """

                echo "[Build] Completed → ${env.IMAGE_TAG}"
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([file(credentialsId: env.ENV_CRED_ID, variable: 'ENV_FILE')]) {
                    sh """
                        set -e

                        echo "--- Copying .env from Jenkins credentials"
                        cp "\$ENV_FILE" ./.env.deploy

                        docker rm -f "${APP_NAME_UNIQUE}" 2>/dev/null || true

                        docker run -d \
                        --name "${APP_NAME_UNIQUE}" \
                        --restart unless-stopped \
                        --env-file ./.env.deploy \
                        --network at-net \
                        ${IMAGE_TAG}

                        rm -f ./.env.deploy
                        echo "--- Deploy OK"
                    """
                }
            }
        }


        stage('Cleanup') {
            steps {
                sh """
                    echo "--- Cleaning old images (keep latest)"
                    OLD_IMAGES=\$(docker images --format "{{.Repository}}:{{.Tag}}" | grep "${APP_NAME}" | grep -v "${IMAGE_TAG}" || true)

                    for IMG in \$OLD_IMAGES; do
                        echo "Deleting: \$IMG"
                        docker rmi -f \$IMG || true
                    done

                    docker image prune -f || true
                """
            }
        }
    }

    post {
        always {
            echo "Pipeline finished for branch ${env.BRANCH_NAME}."
        }
    }
}