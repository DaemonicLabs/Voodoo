node {
	checkout scm
	sh './gradlew clean :builder:build :bootstrap:build'
	archive 'builder/build/libs/*jar'
	archive 'bootstrap/build/libs/*jar'
}