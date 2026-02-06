#!/bin/bash

# =====================================
# Variables
# =====================================
PROJECT_DIR=$(pwd)                     # Répertoire courant TestApp
WEB_CONTENT="$PROJECT_DIR/WebContent" # Dossier contenant les JSP et HTML
WEB_INF="$PROJECT_DIR/WebContent/WEB-INF"        # Dossier WEB-INF
LIB="$WEB_INF/lib"
JAR_DIR="/home/nam/Documents/Reseau/Framework/Framework/lib"

TOMCAT_HOME="/opt/tomcat"
WEBAPPS="$TOMCAT_HOME/webapps"
WAR_NAME="TestApp.war"

# =====================================
# Vérifier que WEB-INF existe
# =====================================
mkdir -p "$LIB"

# =====================================
# Copier le JAR du framework
# =====================================
# Copier tous les .jar de JAR_DIR dans WEB-INF/lib
for jar in "$JAR_DIR"/*.jar; do
    if [ -f "$jar" ]; then
        cp -f "$jar" "$LIB/"
        if [ $? -ne 0 ]; then
            echo "❌ Erreur lors de la copie de $jar !"
            exit 1
        fi
        echo "✅ $jar copié dans $LIB avec succès"
    fi
done


# Créer WEB-INF/classes si inexistant
mkdir -p "$WEB_INF/classes"

# Compiler les classes Java
javac -parameters -cp "$WEB_INF/lib/*" -d "$WEB_INF/classes" $(find ./src -name "*.java")
if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la compilation des classes Java!"
    exit 1
fi
echo "✅ Classes compilées avec succès"

# =====================================
# AJOUT: Copier auth.properties si présent
# =====================================
if [ -f "auth.properties" ]; then
    cp -f "auth.properties" "$WEB_INF/classes/"
    echo "✅ auth.properties copié dans WEB-INF/classes/"
fi

# =====================================
# Créer le WAR correctement
# =====================================
rm -f "$WAR_NAME"

# Inclure WebContent et WEB-INF (avec web.xml)
jar cvf "$WAR_NAME" \
    -C "$WEB_CONTENT" . \
    -C "$WEB_INF" .

if [ $? -ne 0 ]; then
    echo "❌ Erreur lors de la création du WAR!"
    exit 1
fi

echo "✅ WAR créé avec succès : $WAR_NAME"

# =====================================
# Déployer dans Tomcat
# =====================================
rm -f "$WEBAPPS/$WAR_NAME"
cp -f "$WAR_NAME" "$WEBAPPS/"

echo "✅ Application $WAR_NAME déployée dans Tomcat/webapps"

# =====================================
# Redémarrer Tomcat
# =====================================
echo "🔄 Redémarrage de Tomcat..."
"$TOMCAT_HOME/bin/shutdown.sh"
sleep 3
"$TOMCAT_HOME/bin/startup.sh"

echo "====================================="
echo "🚀 Déploiement terminé!"
echo "URL: http://localhost:8080/TestApp"
echo "====================================="