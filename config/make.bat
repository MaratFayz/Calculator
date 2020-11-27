@echo off
cd ..
echo "Start building project..."
echo "Build docker image into local repo"
call mvn clean deploy -P buildDockerImageIntoLocalRepo
echo "Building docker image into local repo finished"

echo "Build services"
docker-compose build
echo "Building services finished"

pause