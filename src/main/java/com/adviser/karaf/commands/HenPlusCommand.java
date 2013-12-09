package com.adviser.karaf.commands;

import henplus.HenPlus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jline.console.ConsoleReader;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.BlueprintContainerAware;
import org.apache.karaf.shell.console.BundleContextAware;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.commands.BlueprintCommand;
import org.apache.karaf.shell.console.completer.CommandsCompleter;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * this class rewrite the action and how the action will be executed
 * Otherwise it manipulates the number and content of arguments/paramter
 * after user types enter from console.
 * Each word typed(after command name) in console is a parameter/ argument. 
 * We can collect all parameters , manipulate them before executing action.  
 * @author toanvu
 *
 */
public class HenPlusCommand extends BlueprintCommand{
	
	private Action action;	
	
	public HenPlus henplus;	
	
	public Completer completer1;
	
	public BundleContext blueprintBundleContext;
	
	private  BlueprintContainer blueprintContainer;	
	
	public void setBlueprintContainer(BlueprintContainer blueprintContainer) {
		this.blueprintContainer = blueprintContainer;
	}

	public void setBlueprintBundleContext(BundleContext context) {
		this.blueprintBundleContext = context;
	}

	public void setHenplus(HenPlus henplus) {
		this.henplus = henplus;
	}
	
	public void setAction(Action action){
		this.action = action;
	}
	
	public void setCompleter1(Completer completer1){
		this.completer1 = completer1;
	}
	
	/**
	 * constructor
	 * Henplus param will be injected from blueprint.xml
	 * @throws IOException 
	 */
	public HenPlusCommand(HenPlus henplus) throws IOException{					
		this.henplus = henplus;
		Completer completer1 = new HenPlusCompleter(this.henplus);
		
		List<Completer> henplusCompleters = new ArrayList<Completer>();		
		henplusCompleters.add(completer1);		
		
		//set first completer
		this.setCompleters(henplusCompleters);		
//		action = new HenPlusCommandAction(this.henplus);
//		createNewAction();
		
	}

	@Override
	public Action createNewAction() {
        if (action instanceof BlueprintContainerAware) {
            ((BlueprintContainerAware) action).setBlueprintContainer(blueprintContainer);
        }
        if (action instanceof BundleContextAware) {
            BundleContext context = (BundleContext) blueprintContainer.getComponentInstance("blueprintBundleContext");
            ((BundleContextAware) action).setBundleContext(context);
        }
        return action;
    }
	
	/**
	 * Do the trick, rewrite argument before do execute
	 * This way prevents the "too many arguments" error.
	 * All arguments from console will be packed together into one argument.
	 * This produced argument will be used in command action
	 * @see HenPlusCommandAction
	 */
	@Override
	public Object execute(CommandSession session, List<Object> arguments) throws Exception {
		try{
			arguments = mergeArguments(arguments);	
			System.out.println("executed command : --"+arguments.get(0)+"--");
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Errors occured by ArgumentParse: "+  e.getMessage());
		}
		
		Action action = createNewAction();
		try {
			if (getPreparator().prepare(action, session, arguments)) {
				return action.execute(session);
			} else {
				return null;
			}
		} finally {
			releaseAction(action);
		}
    }
	
	/**
	 * do a trick, get all passed arguments from console, pack into one argument
	 * @param arguments
	 * @return List with only one Argument
	 */
	private List<Object> mergeArguments(List<Object> rawArguments){
		List<Object> newArguments = new ArrayList<Object>();
		List<String> arguments = handleRawArguments(rawArguments);
		StringBuilder sb = new StringBuilder();		
		int counter = 0;
		for(String argument : arguments){			
			argument = handleStringEqual((String) argument);
			argument = handleStringLike(arguments, (String) argument);
			
			if(counter != 0){
				sb.append(" ").append(argument);
			}else{
				sb.append(argument);
			}			
			counter++;
		}			
		newArguments.add(sb.toString());		
		return newArguments;
	}
	
	
	private String handleStringLike(List<String> arguments, String current){
		int ancestor = 0;
		String result = current;
		String ancestorString = "";		
		if(arguments != null){			
			for(Object object : arguments){			
				if(object.equals(current)){
					break;
				}
				ancestor++;
			}
			
			if(arguments.size() > 0 && ancestor > 0){
				ancestorString =  (String) arguments.get(ancestor-1);
			}
		}		
		
		if(!ancestorString.equalsIgnoreCase("")){
			if(ancestorString.equalsIgnoreCase("like") || ancestorString.equalsIgnoreCase("=")){
				return "'"+current+"'";
			}			
		}		
		
		return result;
	}
	
	private String handleStringEqual(String argument){
		String result = argument;
		if(result.contains("=") && result.length()>1){
			String text = cutOffBackspace(result.split("=")[1]);
			if(!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false") && !text.matches("[+-]?\\d*(\\.\\d+)?")){
				text = "'"+text+"'";
				result = result.split("=")[0]+"="+text;
			}
		}		
		return result;
	}
	
	/**
	 * cut of the backspace in front of object
	 * @param object
	 * @return
	 */
	private String cutOffBackspace(String object){
		while(object.startsWith(" ")){
			object = object.substring(1);
		}
		return object;
	}
	
	/**
	 * handle the rawArguments 
	 * @param rawArguments
	 * @return list of handled arguments or empty list
	 */
	private List<String> handleRawArguments(List<Object> rawArguments){
		List<String> result = new ArrayList<String>();
		if(rawArguments != null){
			for(Object argument : rawArguments){
				if(argument == null){
					argument = "null";
				}
				result.add((String)argument);
			}
		}		
		return result;
	}
	
}
