pipeline {
    agent any
	options {
		buildDiscarder(logRotator(artifactNumToKeepStr: '10'))
	}
	stages {
	    stage("init") {
	        steps {
	            sh 'git submodule update --init --recursive'
	        }
	    }
		stage("test") {
			steps {
                sh './gradlew clean'
                sh './gradlew :voodoo:poet'
				sh './gradlew test -S'
			}
		}
	    stage('publish') {
            steps {
                sh './gradlew publish -S'
            }
        }
	}
	post {
        always {
            sh './gradlew writeMavenUrls'
            script {
               env.URLS = readFile('mavenUrls.txt')
            }
            withCredentials([string(credentialsId: 'discord.webhook.url', variable: 'discordWebhookId')]) {
                discordSend(
                    description: "Downloads: \n${env.URLS}",
                    footer: "build in ${currentBuild.durationString.replace(' and counting', '')}",
                    link: env.BUILD_URL,
                    result: currentBuild.currentResult,
                    title: JOB_NAME,
                    webhookURL: discordWebhookId
                )
            }
        }
        success {
            sh './gradlew buildnumberIncrement'
        }
    }
}