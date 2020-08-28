@echo off
cd ..

echo "Cleaning jar starting"
call mvn clean
echo "Cleaning jar files finished"

echo "Delete services"
docker-compose down
echo "Services are deleted"

pause