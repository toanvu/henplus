package com.adviser.karaf.commands;

import java.util.ArrayList;
import java.util.List;

public class Test {
	
	public static void main(String arg[]){
		String test = "=";
		System.out.println("--"+handleText(test)+"--");
		
		List<String> testList = new ArrayList<String>();
		testList.add("select ");
		testList.add("hello every body");
		testList.add("from");
		System.out.println("descender : of * : " + findDescender(testList, "hello every body"));
	}
	
	private static String findDescender(List<String> arguments, String current){
		int descender = 0;
		String result = current;
		String descenderString = "";
		for(Object o : arguments){
			if(o.equals(current)){
				break;
			}
			descender++;
		}
		if(arguments != null){
			if(arguments.size() > 0 && descender > 0){
				descenderString =  arguments.get(descender-1);
			}
		}		
		
		if(!descenderString.equalsIgnoreCase("")){
			if(descenderString.equalsIgnoreCase("like") || descenderString.equalsIgnoreCase("=")){
				return "'"+current+"'";
			}			
		}		
		
		return result;
	}

	private static String handleText(String argument){
		String result = argument;
		if(result.contains("=") && result.length()>1){
			String text = cutOffBackspace(result.split("=")[1]);
			if(!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")){
				text = "'"+text+"'";
				result = result.split("=")[0]+"="+text;
			}
		}		
		return result;
	}
	
	private static String cutOffBackspace(String object){
		while(object.startsWith(" ")){
			object = object.substring(1);
		}
		return object;
	}
}
