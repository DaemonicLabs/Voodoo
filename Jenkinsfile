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
	            sh './gradlew :multimc-installer:minify'
	            archive 'multimc-installer/build/libs/*jar'
	        }
	    }
	    stage("server-installer") {
	        steps {
	            sh './gradlew :server-installer:clean'
	            sh './gradlew :server-installer:minify'
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
	    /* stage("bootstrap") {
	        steps {
	            sh './gradlew :bootstrap:clean'
	            sh './gradlew :bootstrap:minify -Ptarget=voodoo'
	            sh './gradlew :bootstrap:minify -Ptarget=multimc-installer'
	            sh './gradlew :bootstrap:minify -Ptarget=archiver'
	            archive 'bootstrap/build/libs/*voodoo*'
	            archive 'bootstrap/build/libs/*multimc-installer*'
	            archive 'bootstrap/build/libs/*archiver*'
	        }
	    } */
	}
}