module aeonics.core
{
	requires java.compiler;
	requires jdk.management;
	requires java.sql;
	requires aeonics.boot;
	
	exports aeonics.data;
	exports aeonics.entity;
	exports aeonics.entity.security;
	exports aeonics.manager;
	exports aeonics.template;
	exports aeonics.util;
	
	provides aeonics.Plugin with local.Main;
}
