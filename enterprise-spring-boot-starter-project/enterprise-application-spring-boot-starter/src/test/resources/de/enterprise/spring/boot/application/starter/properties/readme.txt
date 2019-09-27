keytool -genkey -alias test -keyalg RSA -storetype jceks -keystore keystore.jce -keysize 2048

keytool -genkeypair -alias mytestkey -keyalg RSA -dname "CN=App,OU=Ves,O=Veolia,L=Hamburg,S=Hamburg,C=DE" -keypass ttves -keystore keystore.jks -storepass letmein -keysize 2048