pipeline {
    agent any
	stages {
	    stage("voodoo") {
	        steps {
	            sh './gradlew :voodoo:clean'
	            sh './gradlew :voodoo:build'
	            archive 'voodoo/build/libs/*jar'
	        }
	    }
	    stage("hex") {
	        steps {
	            sh './gradlew :hex:clean'
	            sh './gradlew :hex:build'
	            archive 'hex/build/libs/*jar'
	        }
	    }
	    stage("bootstrap") {
	        steps {
	            sh './gradlew :bootstrap:clean :bootstrap:build -Ptarget=voodoo'
	            archive 'voodoo/build/libs/voodoo*jar'
	            sh './gradlew :bootstrap:clean :bootstrap:build -Ptarget=hex'
	            archive 'voodoo/build/libs/hex*jar'
	        }
	    }
	}
}