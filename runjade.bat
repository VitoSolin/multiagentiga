@echo off
echo Starting JADE with SellerAgent...
java -cp "lib/jade.jar;." jade.Boot -gui -agents "seller:SellerAgent"
pause 