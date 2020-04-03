#!/bin/bash

# The sole purpose of this file is to facilitate the compilation and running of each provided project.

VALID_ARGS=(keys keys_config kms kms_config postgres example example_config)
USAGE="\033[1;34m
This program compiles and runs one of several sample deliverables provided by Oracle for Switchfly.\n
Usage: ./run.sh PROGRAM_ID\n
Valid PROGRAM_IDs:\n
${VALID_ARGS[*]}\033[0m"
declare -A FOLDERS
FOLDERS["${VALID_ARGS[0]}"]="Create_Rotate_Keys"; FOLDERS["${VALID_ARGS[1]}"]="Create_Rotate_Keys"
FOLDERS["${VALID_ARGS[2]}"]="KMS_Crypto"; FOLDERS["${VALID_ARGS[3]}"]="KMS_Crypto"
FOLDERS["${VALID_ARGS[4]}"]="Postgres_Storage"
FOLDERS["${VALID_ARGS[5]}"]="Example"; FOLDERS["${VALID_ARGS[6]}"]="Example"
declare -A FILENAMES
FILENAMES["${VALID_ARGS[0]}"]="Keys"; FILENAMES["${VALID_ARGS[1]}"]="KeysConfig"
FILENAMES["${VALID_ARGS[2]}"]="KmsCrypto"; FILENAMES["${VALID_ARGS[3]}"]="KmsCryptoConfig"
FILENAMES["${VALID_ARGS[4]}"]="PostgresStore"
FILENAMES["${VALID_ARGS[5]}"]="Demo"; FILENAMES["${VALID_ARGS[6]}"]="DemoConfig"

if [ "$#" -gt 1 ]; then
	echo -e "\033[1;31mERROR: Too many arguments.\033[0m"
	echo -e $USAGE
	exit
elif [ "$#" -eq 0 ]; then
	echo -e "\033[1;31mERROR: No argument provided.\033[0m"
	echo -e $USAGE
	exit
else
	if [[ " "${VALID_ARGS[@]}" " == *" "$1" "* ]]; then
		echo -e "\033[1;30mArgument \"$1\" accepted. Continuing...\033[0m"
	else
		echo -e "\033[1;31mERROR: Invalid argument provided.\033[0m"
		echo -e $USAGE
		exit
	fi
fi

# This line details the explicit libraries compiled
LIBRARIES="lib/oci-java-sdk-full-1.12.0.jar:lib/javax.activation-1.2.0.jar:lib/slf4j-jdk14-1.7.30.jar:lib/third-party/lib/*"
LIBRARIES2="lib/postgresql-42.2.9.jar"

compartmentId=""
vaultId=""
region=""

if [ "$1" == "postgres" ]; then
	LIBRARIES=$LIBRARIES2
else
	# We will need to grab these three variables from the command line and pass them into the java files
	#    compartmentId (regexp: ocid1.compartment.oc1..[a-z0-9]{60})
	#    vaultId (regexp: ocid1.vault.oc1.[REGION_IDENTIFIER].[VAULT_IDENTIFIER].[a-z0-9]{60})
	#    region (looks something like US_PHOENIX_1)
	# TODO: PLEASE NOTE THAT THIS SECTION REFERENCES OCIDs AS THEY WERE AS OF JANUARY 2020.
	# THE FORMAT AND/OR DOMAIN OF OCIDs MAY CHANGE AND/OR EXPAND IN THE FUTURE. IF SO, SKIP
	# THIS PART AND UNCOMMENT THE SIX LINES BELOW.
	#################################################
	#echo -e 'Please paste the compartmentId (regexp: "ocid1.compartment.oc1..[a-z0-9]{60}")'
	#read compartmentId
	#echo -e 'Please paste the vaultId (regexp: "ocid1.vault.oc1.[a-z]{3}.[a-z0-9]{10,16}.[a-z0-9]{60}")'
	#read vaultId
	#echo -e 'Please paste the region (form "US_ASHBURN_1" or "AP_TOKYO_1" or similar)'
	#read region
	##################################################
	tries=0
	MAX_TRIES=3
	# pull the compartment ID from stdin, with a max of MAX_TRIES (3) attempts
	until [[ $compartmentId =~ ocid1.compartment.oc1..[a-z0-9]{60} ]] || [[ $tries -ge $MAX_TRIES ]]; do
		echo -e 'Please paste the compartmentId (regexp: "ocid1.compartment.oc1..[a-z0-9]{60}")'
		read compartmentId
		tries=$(($tries+1))
	done
	if [[ $tries -ge $MAX_TRIES ]]; then
		echo -e "\033[1;31mError: invalid compartment ID. Please make sure that you have copied a valid compartment ID.\033[0m"
		exit
	else
		tries=0
		echo -e "    Compartment ID set to \"$compartmentId\". Press \"CTRL+C\" if this is not the case to interrupt this script."
	fi
	# pull the vault ID from stdin, with a max of MAX_TRIES (3) attempts
	until [[ $vaultId =~ ocid1.vault.oc1.[a-z]{3}.[a-z0-9]{10,16}.[a-z0-9]{60} ]] || [[ $tries -ge $MAX_TRIES ]]; do # idk the VAULT_IDENTIFIER part, ours has 13 characters
		echo -e 'Please paste the vaultId (regexp: "ocid1.vault.oc1.[a-z]{3}.[a-z0-9]{10,16}.[a-z0-9]{60}")'
		read vaultId
		tries=$(($tries+1))
	done
	if [[ $tries -ge $MAX_TRIES ]]; then
		echo -e "\033[1;31mError: invalid vault ID. Please make sure that you have copied a valid vault ID.\033[0m"
		exit
	else
		tries=0
		echo -e "    Vault ID set to \"$vaultId\". Press \"CTRL+C\" if this is not the case to interrupt this script."
	fi
	# pull the region from stdin, with a max of MAX_TRIES (3) attempts
	until [[ $region =~ (ap|ca|eu|sa|uk|us)-[a-z_]{5,11}-1 ]] || [[ $tries -ge $MAX_TRIES ]]; do
		echo -e 'Please paste the region String code (form "us-ashburn-1" or "ap-tokyo-1" or similar)'
		read region
		tries=$(($tries+1))
	done
	if [[ $tries -ge $MAX_TRIES ]]; then
		echo -e "\033[1;31mError: invalid region. Please make sure that you have copied a valid region.\033[0m"
		exit
	else
		echo -e "    Region set to \"$region\". Press \"CTRL+C\" if this is not the case to interrupt this script."
	fi
	# If example, then link both the OCI+3p libraries and the postgres library; otherwise only the OCI+3p libraries need to be used (already set)
	if [ "$1" == "example" ] || [ "$1" == "example_config" ]; then
		LIBRARIES=$LIBRARIES:$LIBRARIES2
	fi
fi

FOLDER="${FOLDERS[$1]}"
FILENAME="${FILENAMES[$1]}"

echo -e "\n\033[1;30mCompiling $FILENAME...\033[0m\n"
echo -e "Command: javac -cp $LIBRARIES $FOLDER/$FILENAME"'.java'
javac -cp $LIBRARIES $FOLDER/$FILENAME'.java' || {
	# if the javac command fails, this happens instead
	echo -e "\033[1;31mERROR: compilation failed. Please view above logs and fix the compilation issues.\033[0m"
	exit
}

echo -e "\n\033[1;30mRunning $FILENAME...\033[0m\n"
echo -e "Command: java -cp $FOLDER:$LIBRARIES $FILENAME $compartmentId $vaultId $region"
java -cp $FOLDER:$LIBRARIES $FILENAME $compartmentId $vaultId $region || { # if example* is not the arg then the command will execute as if there were no command line arguments
	echo -e "\033[1;31mERROR: program has crashed. Please fix the bug(s) before trying again.\033[0m"
	exit
}

echo -e "\n\033[1;32mDone.\033[0m\n"

# Example of running the full command for KmsCrypto:
# javac -cp lib/oci-java-sdk-full-1.12.0.jar:lib/third-party/lib/*:lib/javax.activation-1.2.0.jar:lib/slf4j-jdk14-1.7.30.jar KmsCrypto/KmsCrypto.java
# java -cp KmsCrypto:lib/oci-java-sdk-full-1.12.0.jar:lib/third-party/lib/*:lib/javax.activation-1.2.0.jar:lib/slf4j-jdk14-1.7.30.jar KmsCrypto
