# Darbaan
Darbaan is a gateway library used in channel nodes in order to facilitate talk to the safir nodes.

## Build
You need jdk >= 1.8 and maven to build darbaan. simply use maven to build and install the artifact 
into your local repository by the command:
```
mvn install
```
Then you can add darbaan into your project POM file like this:
```
<dependency>
        <groupId>com.piran-framework</groupId>
        <artifactId>darbaan</artifactId>
        <version>0.1-SNAPSHOT</version>
</dependency>
```

## Contribution
Any contributions are welcomed. Also, if you have any issues using Darbaan, you can create an issue on 
Github issue tracker of the project. There is just one limitation for contributions, and it is 
respecting the code style located in code-style.xml

## License
Copyright (c) 2018 Isa Hekmatizadeh.

Darbaan is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser 
General Public License as published by the Free Software Foundation, either version 3 of the 
License, or (at your option) any later version.

Darbaan is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU Lesser General 
Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
