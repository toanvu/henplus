package com.adviser.karaf.commands;

import jline.console.ConsoleReader;
import henplus.HenPlus;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * this class takes all parameters and execute the logic function
 * @author toanvu
 *
 */
@Command(scope = "henplus", name = "sql", description = "Says hello")
public class HenPlusCommandAction implements Action{
	@Argument(index = 0, name = "sqlCmd", description = "The command argument", required = false, multiValued = false)
	String sqlCmd = null;

	private HenPlus henplus;    
	
	private BundleContext context;
	
    public HenPlusCommandAction(HenPlus henplus){
    	this.setHenplus(henplus);    	
    }   
            
    public void setContext(BundleContext context) {
		this.context = context;
	}

	public void setHenplus(HenPlus henplus){
    	this.henplus = henplus;
    }

	@Override
	public Object execute(CommandSession session) throws Exception {		
		if(sqlCmd != null){
			if(!sqlCmd.endsWith(";")){
				sqlCmd += ";";
			}			
			System.out.println(henplus.getPrompt());			
			henplus.run(sqlCmd);			
		}else{
			System.err.println("Argument is missing ");
		}
		return null;
	}		

}
