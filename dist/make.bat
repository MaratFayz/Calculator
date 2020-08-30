@echo off
cd ..
echo "Start building project..."
echo "Build jar file"
call mvn clean package
echo "Building jar files finished"

echo "Build services"
docker-compose build
echo "Building services finished"

pause