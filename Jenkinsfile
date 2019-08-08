pipeline {
    agent any
    // environment {
    //     GRADLE_OPTS = '-Dkotlin.compiler.execution.strategy="in-process"'
    // }
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
				sh './gradlew test -S'
			}
		}
	    stage('publish') {
            steps {
                sh './gradlew publish -S'
            }
        }
		stage('counter') {
			steps {
				sh './gradlew buildnumberIncrement'
                withCredentials([string(credentialsId: 'discord.webhook.url', variable: 'discordWebhookId')]) {
                    discordSend description: 'Jenkins Pipeline Build', footer: 'Footer Text', link: env.BUILD_URL, result: currentBuild.currentResult, title: JOB_NAME, webhookURL: discordWebhookId
                }
			}
		}
	}
}