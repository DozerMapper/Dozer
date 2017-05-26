[![Build Status](https://travis-ci.org/DozerMapper/dozer.svg?branch=master)](https://travis-ci.org/DozerMapper/dozer)
[![Dependency Status](https://www.versioneye.com/java/net.sf.dozer:dozer/badge?style=flat-square)](https://www.versioneye.com/java/net.sf.dozer:dozer)
[![Release Version](https://img.shields.io/maven-central/v/net.sf.dozer/dozer.svg?maxAge=2592000)](https://mvnrepository.com/artifact/net.sf.dozer/dozer)
[![License](https://img.shields.io/hexpm/l/plug.svg?maxAge=2592000)]()

# Dozer
## Looking for HELP
We are actively looking for people who want to help with the Dozer project!

## Why Map?
A mapping framework is useful in a layered architecture where you are creating layers of abstraction by encapsulating changes to particular data objects vs. propagating these objects to other layers (i.e. external service data objects, domain objects, data transfer objects, internal service data objects).

Mapping between data objects has traditionally been addressed by hand coding value object assemblers (or converters) that copy data between the objects. Most programmers will develop some sort of custom mapping framework and spend countless hours and thousands of lines of code mapping to and from their different data object.

This type of code for such conversions is rather boring to write, so why not do it automatically?


## What is Dozer?
Dozer is a Java Bean to Java Bean mapper that recursively copies data from one object to another, it is an open source mapping framework that is robust, generic, flexible, reusable, and configurable.

Dozer supports simple property mapping, complex type mapping, bi-directional mapping, implicit-explicit mapping, as well as recursive mapping. This includes mapping collection attributes that also need mapping at the element level.

Dozer not only supports mapping between attribute names, but also automatically converting between types. Most conversion scenarios are supported out of the box, but Dozer also allows you to specify custom conversions via XML or code-based configuration.

## Getting Started
Check out the [Getting Started Guide](http://dozer.sf.net/documentation/gettingstarted.html) and [Full User Guide](http://dozer.sf.net/dozer-user-guide.pdf) or [Dozer SF Site](http://dozer.sf.net) for advanced information.

## Getting the Distribution
If you are using Maven, simply copy-paste this dependency to your project.

```XML
<dependency>
    <groupId>com.github.dozermapper</groupId>
    <artifactId>dozer-core</artifactId>
    <version>6.0.0</version>
</dependency>
```

## Simple Example
```XML
<mapping>
  <class-a>yourpackage.SourceClassName</class-a>
  <class-b>yourpackage.DestinationClassName</class-b>
    <field>
      <A>yourSourceFieldName</A>
      <B>yourDestinationFieldName</B>
    </field>
</mapping>
```

```Java
SourceClassName sourceObject = ...

Mapper mapper = new DozerBeanMapper();
DestinationObject destObject = mapper.map(sourceObject, DestinationClassName.class);

assertTrue(destObject.getYourDestinationFieldName().equals(sourceObject.getYourSourceFieldName));
```
