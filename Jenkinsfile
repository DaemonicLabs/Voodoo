node {
	checkout scm
	sh './gradlew clean :voodoo:build :bootstrap:build'
	archive 'voodoo/build/libs/*jar'
	archive 'bootstrap/build/libs/*jar'
}