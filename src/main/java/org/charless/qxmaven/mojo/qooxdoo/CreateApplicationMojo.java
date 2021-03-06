package org.charless.qxmaven.mojo.qooxdoo;


import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.charless.qxmaven.mojo.qooxdoo.app.Application;
import org.w3c.dom.Document;

/**
 * Create a new application 
 * 
 * @goal create-application
 * @execute phase="initialize"
 * @requiresDirectInvocation true
 */
public class CreateApplicationMojo extends AbstractPythonMojo {
	
	static {
		setSCRIPT_NAME("create-application.py");
	}
	
	/** 
	 * Type of the application to create
	 * 
	 * ['contribution', 'desktop', 'inline', 'mobile','native', 'server', 'website']
	 * @parameter   expression="${type}"
     * 				default-value="desktop"
     * @required
	 */
	private String type;
	
	/**
	 * EXPERIMENTAL !
	 * 
	 * A directory containing an existing non-mavenized Qooxdoo application.
	 * Useful if you want to "mavenize" an existing application.
	 * 
	 * Make sure the qooxdoo.application.namespace is set to the imported application namespace
	 * It's up to you to check that the imported application is compatible with the qooxdooSdk version
	 * 
	 * Note: in that case the type attribute is useless
	 * @parameter   expression="${fromDirectory}"
	 * 
	 */
	private File fromDirectory;
	
    /**
     * Path where the qooxdoo application will be created, right after the create-application.py call
     * Please note this is not the final application directory
     * 
     * @parameter   expression="${qooxdoo.application.tmpDirectory}"
     * 				default-value="${project.build.directory}/qooxdoo/tmp"
     * @required
     */
    private File tmpDirectory;
	
	@Override
    public void execute() throws MojoExecutionException {
		// Check
		this.tmpDirectory.mkdirs();
		try {
			FileUtils.deleteDirectory(this.getTmpApplicationTarget());
		} catch (Exception e) {
			getLog().warn("Could not delete directory "+this.getTmpApplicationTarget());
		}
		// Check if a previous app exist
		File targetSourceDir = new File(sourcesDirectory,namespace);
		if (targetSourceDir.isDirectory()) {
			throw new MojoExecutionException("Looks like the application already exists ! You must remove the directory '"+targetSourceDir.getAbsolutePath()+"' to create a new application");
		}
			
		// Create app
		File appdir = null;
		if (this.fromDirectory == null) {
			getLog().info("Creating a '"+type+"' application named '"+namespace+"'...");
			try {
				if (useEmbeddedJython) 	{ jython(); } 
				else 					{ python(); }
			} catch (Exception e) {
				throw new MojoExecutionException(e.getMessage());
			}
			appdir = getTmpApplicationTarget();
			if (! (appdir.exists()&&appdir.canRead()&&appdir.isDirectory()) ) {
				throw new MojoExecutionException("Looks like the application has not been created into "+appdir.getAbsolutePath());
			}
		} else {
			appdir = this.fromDirectory;
			if (! (appdir.exists()&&appdir.canRead()&&appdir.isDirectory()) ) {
				throw new MojoExecutionException("Looks like the application directory specified by the fromDirectory parameter is not an existing directory "+appdir.getAbsolutePath());
			}
		}
		// Migrate to a maven structure
		getLog().info("Mavenizing '"+appdir.getAbsolutePath()+"' ...");
		Application app = new Application(appdir);
		app.setMvnSourcesDirectory(sourcesDirectory);
		app.setMvnResourcesDirectory(resourcesDirectory);
		app.setMvnTestDirectory(testDirectory);
		app.setMvnTranslationDirectory(translationDirectory);
		try {
			app.mavenize();
		} catch (Exception e) {
			throw new MojoExecutionException(e.getMessage());
		}
		getLog().info("Application '"+namespace+"' has been created successfuly !");

    }
	
	 protected String[] getCommandLineOptions() {
	    return new String[]{"-n",namespace,"-t",type,"-o",this.getTmpApplicationTarget().getParentFile().getAbsolutePath()};
	 }
	 
	 private File getTmpApplicationTarget() {
		 return new File(this.tmpDirectory,this.namespace);
	 }
	 
}
