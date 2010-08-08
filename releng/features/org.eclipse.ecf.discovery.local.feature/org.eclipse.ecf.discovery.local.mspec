<?xml version="1.0" encoding="UTF-8"?>
<md:mspec xmlns:md="http://www.eclipse.org/buckminster/MetaData-1.0" 
    name="org.eclipse.ecf.discovery.local" 
    materializer="p2" 
    url="org.eclipse.ecf.discovery.local.cquery">
    
    <md:mspecNode namePattern="^org\.eclipse\.ecf\.discovery\.local\.feature?" materializer="workspace"/>
    <md:mspecNode namePattern="^org\.eclipse\.ecf\.provider\.localdiscovery?" materializer="workspace"/>
    <md:mspecNode namePattern="^org\.eclipse\.ecf\.osgi\.services\.discovery\.local?" materializer="workspace"/>
    
    <md:mspecNode namePattern="^org\.eclipse\.ecf\.tests\.osgi\.services\.discovery\.local(\..+)?" materializer="workspace"/>
    <md:mspecNode namePattern="^org\.eclipse\.ecf\.tests\.osgi\.services\.distribution\.localdiscovery?" materializer="workspace"/>

    <md:mspecNode namePattern=".*" installLocation="${targetPlatformPath}"/>
</md:mspec>
	
