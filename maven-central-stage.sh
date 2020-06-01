#/bin/bash
set -e

# A space delimited list of feature IDs
features_whose_bundles_we_will_deploy="org.eclipse.ecf.remoteservice.sdk.feature"

# Exclude orbit bundles, they should be done separately
orbit_bundles="javax.servlet
org.apache.commons.codec
org.apache.commons.logging
org.apache.hadoop.zookeeper
org.apache.httpcomponents.httpclient
org.apache.httpcomponents.httpcore
org.apache.log4j
org.json
org.objectweb.asm"

function deploy() {
	local target=$1
	mkdir -p $target
	sed -e 's/-SNAPSHOT//' $(dirname $target)/pom.xml > $target/pom.xml
	local pom=$target/pom.xml
	local file="$(ls $target/*-SNAPSHOT.jar 2>/dev/null)"
	local sources="${file%.jar}-sources.jar"
	local javadoc="${file%.jar}-javadoc.jar"

	# Create dummy javadoc jar
	if [ -f "$file" -a ! -f "$javadoc" ] ; then
		echo "Javadoc for ECF is distributed separately, please see: http://download.eclipse.org/rt/ecf/latest/javadoc/" > $target/README.txt
		(cd $target && jar -cf $(basename $javadoc) README.txt)
	fi

	# Build properties list
	local props="-DpomFile=$pom"
	if [ -n "$file" ] ; then
		props="$props -Dfile=$file"
	else
		props="$props -Dfile=$pom"
	fi
	if [ -f "$sources" ] ; then
		props="$props -Dsources=$sources"
	fi
	if [ -f "$javadoc" ] ; then
		props="$props -Djavadoc=$javadoc"
	fi

	#local settings=/opt/public/hipp/homes/genie.ecf/.m2/settings-deploy-ossrh.xml
	local settings=~/.m2/settings.xml
	echo "mvn -s $settings gpg:sign-and-deploy-file -DrepositoryId=ossrh " \
	  "-Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/" \
	  "$props -Dtycho.mode=maven"
	mvn -s $settings gpg:sign-and-deploy-file -DrepositoryId=ossrh \
	  -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ \
	  $props -Dtycho.mode=maven
}

bundles=""
function _add_bundle_to_list() {
	local plugin=$1
	local match=0
	for b in $bundles; do
		if [ "$b" = "$plugin" ] ; then
			match=1
		fi
	done
	if [ "$match" != "1" ] ; then
		echo "    $plugin"
		local orbit_match=0
		for o_b in $orbit_bundles ; do
			if [ "$o_b" = "$plugin" ] ; then
				orbit_match=1
				break
			fi
		done
		if [ "$orbit_match" != "1" ] ; then
			bundles="$bundles $plugin"
		fi
	fi
	return $match
}

features=""
function _add_feature_to_list() {
	local feature=$1
	local match=0
	for f in $features ; do
		if [ "$f" = "$feature" ] ; then
			match=1
		fi
	done
	if [ "$match" != "1" ] ; then
		echo "  $feature"
		features="$features $feature"
	fi
	return $match
}

function xpath_call() {
	local xmlfile=$1
	local xmlquery=$2
	local result=
	if [ -n "$JENKINS_NAME" ] ; then
		result=$(xpath $xmlfile "$xmlquery")
	else
		result=$(xpath -e "$xmlquery" $xmlfile 2>/dev/null)
	fi
	echo "$result"
}

function parse_feature() {
	local xml=$(find -path "*/$1/feature.xml")

	# Process included plugins
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local plugin=$(xpath_call $xml "string(//plugin[$i]/@id)")
		if [ -z "$plugin" ] ; then
			break
		else
			_add_bundle_to_list $plugin || :
		fi
	done

	# Process plugins specified as "requires"
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local entry=$(xpath_call $xml "//requires/import[$i]")
		if [ -z "$entry" ] ; then
			break
		fi
		local plugin=$(xpath_call $xml "string(//requires/import[$i]/@plugin)")
		set +x
		if [ -n "$plugin" ] ; then
			_add_bundle_to_list $plugin || :
		fi
	done

	# Process included features
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local subfeature=$(xpath_call $xml "string(//includes[$i]/@id)")
		if [ -z "$subfeature" ] ; then
			break
		else
			_add_feature_to_list $subfeature && parse_feature $subfeature
		fi
	done

	# Process features specified as "requires"
	local i=0
	while true ; do
		i=$(( $i + 1 ))
		local entry=$(xpath_call $xml "//requires/import[$i]")
		if [ -z "$entry" ] ; then
			break
		fi
		local subfeature=$(xpath_call $xml "string(//requires/import[$i]/@feature)")
		if [ -n "$subfeature" ] ; then
			_add_feature_to_list $subfeature && parse_feature $subfeature
		fi
	done
}

bundles_deploy=""
function check_maven_central() {
	local central_version=$(curl https://repo1.maven.org/maven2/org/eclipse/ecf/$1/maven-metadata.xml 2>/dev/null | grep '<latest>' | sed -e 's/.*>\(.*\)<.*/\1/')
	local jar_version=$(find -name "$1-*-SNAPSHOT.jar" | tail -n1 | sed -e "s/.*$1-\(.*\)-SNAPSHOT.jar/\1/")
	local op="="
	if [ $(echo $jar_version | cut -f1 -d.) -gt $(echo $central_version | cut -f1 -d.) ] ; then
		op=">"
	elif [ $(echo $jar_version | cut -f2 -d.) -gt $(echo $central_version | cut -f2 -d.) ] ; then
		op=">"
	elif [ $(echo $jar_version | cut -f3 -d.) -gt $(echo $central_version | cut -f3 -d.) ] ; then
		op=">"
	fi
	echo "  $1 $jar_version $op $central_version"
	if [ "$op" != "=" ] ; then
		bundles_deploy="$bundles_deploy $1"
	fi
}

echo
echo "Parsing the following features:"
echo "==============================="
for feature in $features_whose_bundles_we_will_deploy ; do
	_add_feature_to_list $feature && parse_feature $feature
done

echo
echo "Checking the following found bundles against maven central:"
echo "==========================================================="
bundles_sorted="$(for b in $bundles ; do echo $b ; done | sort)"
for bundle in $bundles_sorted ; do
	check_maven_central $bundle
done

echo
echo "Will deploy the following bundles with newer versions:"
echo "======================================================"
for bundle in $bundles_deploy ; do
	echo "  $bundle"
done

# Deploy parent pom
#deploy "./target"

# Deploy all discovered bundles
echo
for b in $bundles_deploy ; do
	jars=$(find -name "$b-*-SNAPSHOT.jar" | tail -n1)
	for jar in $jars ; do
		deploy "$(dirname $jar)"
	done
done
