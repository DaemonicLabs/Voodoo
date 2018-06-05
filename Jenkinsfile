pipeline {
    agent any
	stages {
	    stage("init") {
	        steps {
	            sh 'git submodule update --init --recursive'
	        }
	    }
	    stage("voodoo") {
	        steps {
	            sh './gradlew :voodoo:clean'
	            sh './gradlew :voodoo:build'
	            archive 'voodoo/build/libs/*jar'
	        }
	    }
	    stage("multimc-installer") {
	        steps {
	            sh './gradlew :multimc-installer:clean'
	            sh './gradlew :multimc-installer:build'
	            archive 'multimc-installer/build/libs/*jar'
	        }
	    }
	    stage("server-installer") {
	        steps {
	            sh './gradlew :server-installer:clean'
	            sh './gradlew :server-installer:build'
	            archive 'server-installer/build/libs/*jar'
	        }
	    }
	    stage("archiver") {
	        steps {
	            sh './gradlew :archiver:clean'
	            sh './gradlew :archiver:build'
	            archive 'archiver/build/libs/*jar'
	        }
	    }
	    stage("bootstrap") {
	        steps {
	            sh './gradlew :bootstrap:clean'
	            sh './gradlew :bootstrap:build -Ptarget=voodoo'
	            sh './gradlew :bootstrap:build -Ptarget=multimc-installer'
	            sh './gradlew :bootstrap:build -Ptarget=archiver'
	            archive 'bootstrap/build/libs/*voodoo*'
	            archive 'bootstrap/build/libs/*multimc-installer*'
	            archive 'bootstrap/build/libs/*archiver*'
	        }
	    }
	}
}