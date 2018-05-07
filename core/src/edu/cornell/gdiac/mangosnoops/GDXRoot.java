/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game.  It is the "static main" of
 * LibGDX.  In the first lab, we extended ApplicationAdapter.  In previous lab
 * we extended Game.  This is because of a weird graphical artifact that we do not
 * understand.  Transparencies (in 3D only) is failing when we use ApplicationAdapter. 
 * There must be some undocumented OpenGL code in setScreen.
 *
 * This time we shown how to use Game with player modes.  The player modes are 
 * implemented by screens.  Player modes handle their own rendering (instead of the
 * root class calling render for them).  When a player mode is ready to quit, it
 * notifies the root class through the ScreenListener interface.
 *
 * Author: Walker M. White
 * Based on original Optimization Lab by Don Holden, 2007
 * LibGDX version, 2/2/2015
 */
package edu.cornell.gdiac.mangosnoops;
import com.badlogic.gdx.graphics.Pixmap;
import edu.cornell.gdiac.mangosnoops.Menus.SettingsMenu;
import edu.cornell.gdiac.mangosnoops.Menus.StartMenuMode;
import edu.cornell.gdiac.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.*;
import com.badlogic.gdx.assets.loaders.*;
import com.badlogic.gdx.assets.loaders.resolvers.*;

/**
 * Root class for a LibGDX.  
 * 
 * This class is technically not the ROOT CLASS. Each platform has another class above
 * this (e.g. PC games use DesktopLauncher) which serves as the true root.  However, 
 * those classes are unique to each platform, while this class is the same across all 
 * plaforms. In addition, this functions as the root class all intents and purposes, 
 * and you would draw it as a root class in an architecture specification.  
 */
public class GDXRoot extends Game implements ScreenListener {
	/** AssetManager to load game assets (textures, sounds, etc.) */
	private AssetManager manager;
	/** Drawing context to display graphics (VIEW CLASS) */
	private GameCanvas canvas; 
	/** Player mode for the asset loading screen (CONTROLLER CLASS) */
	private LoadingMode loading;
	/** Player mode for the the game proper (CONTROLLER CLASS) */
	private GameMode    playing;
	private RestStopMode reststop;
	private StartMenuMode start;
	private SettingsMenu settings;
	private SoundController soundController;

	// LEVEL FILES TODO implement moving to next level
	private static final String[] LEVELS = new String[]{"tut0.xlsx", "level0.xlsx", "level1.xlsx"};
	private static int currLevel;

	/**
	 * Creates a new game from the configuration settings.
	 *
	 * This method configures the asset manager, but does not load any assets
	 * or assign any screen.
	 */
	public GDXRoot() {
		// Start loading with the asset manager
		manager = new AssetManager();
		
		// Add font support to the asset manager
		FileHandleResolver resolver = new InternalFileHandleResolver();
		manager.setLoader(FreeTypeFontGenerator.class, new FreeTypeFontGeneratorLoader(resolver));
		manager.setLoader(BitmapFont.class, ".ttf", new FreetypeFontLoader(resolver));
		currLevel = 0;
	}


	/** 
	 * Called when the Application is first created.
	 * 
	 * This is method immediately loads assets for the loading screen, and prepares
	 * the asynchronous loader for all other assets.
	 */
	public void create() {
		currLevel = 0;
		Gdx.graphics.setTitle("Home Away From Gnome");
		setCursor("images/mouse.png");
		canvas  = new GameCanvas();
		loading = new LoadingMode(canvas,manager,1);
		settings = new SettingsMenu(this);
		soundController = new SoundController(settings);
		playing = new GameMode(canvas,settings,soundController,LEVELS[currLevel]);
		reststop = new RestStopMode(canvas, manager);
		start = new StartMenuMode(canvas, manager,settings,soundController);

		loading.setScreenListener(this);
		playing.preLoadContent(manager); // Load game assets statically.
		settings.preLoadContent(manager);
		setScreen(loading);
	}

	/** 
	 * Called when the Application is destroyed. 
	 *
	 * This is preceded by a call to pause().
	 */
	public void dispose() {
		// Call dispose on our children
		Screen screen = getScreen();
		setScreen(null);
		screen.dispose();
		canvas.dispose();
		canvas = null;
	
		// Unload all of the resources
		manager.clear();
		manager.dispose();
		super.dispose();
	}
	
	/**
	 * Called when the Application is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to create().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		canvas.resize();
		super.resize(width,height);
	}
	
	/**
	 * The given screen has made a request to exit its player mode.
	 *
	 * The value exitCode can be used to implement menu options.
	 *
	 * @param screen   The screen requesting to exit
	 * @param exitCode The state of the screen upon exit
	 */
	public void exitScreen(Screen screen, int exitCode) {
		if (exitCode != 0) {
			Gdx.app.error("GDXRoot", "Exit with error code "+exitCode, new RuntimeException());
			Gdx.app.exit();
		} else if (screen == loading) {
			playing.loadContent(manager);
			settings.loadContent(manager);
			start.setScreenListener(this);
			Gdx.input.setInputProcessor(start);
			setScreen(start);
			loading.dispose();
			loading = null;
		} else if (screen == start) {
			if(start.levelSelectButtonClicked()) {

			} else if(start.exitButtonClicked()) {
				Gdx.app.exit();
			} else if(start.settingsButtonClicked()) {
			} else {
				playing.setScreenListener(this);
				//Gdx.input.setInputProcessor(playing);
				setScreen(playing);
				start.dispose();
				start = null;
			}
		} else if (screen == playing) {
			if(playing.exitFromPause){
				playing.exitFromPause = false;
				start = new StartMenuMode(canvas,manager,settings,soundController);
				start.setScreenListener(this);
				Gdx.input.setInputProcessor(start);
				setScreen(start);
				//playing.dispose();
				//playing = null;
			} else {
				reststop = new RestStopMode(canvas, manager);
				reststop.setPlayerInv(playing.getInventory());
				reststop.setScreenListener(this);
				Gdx.input.setInputProcessor(reststop);
				setScreen(reststop);
				playing.dispose();
				playing = null;
			}

		} else if (screen == reststop) {
			currLevel = (currLevel + 1) % LEVELS.length; // TODO : something that will end the game at last level
			playing = new GameMode(canvas,settings,soundController,LEVELS[currLevel]);
			playing.preLoadContent(manager);
			playing.loadContent(manager);
			playing.setInventory(reststop.getPlayerInv()); // manually set inventory bc new GameMode
			playing.setScreenListener(this);
			//Gdx.input.setInputProcessor(playing);
			setScreen(playing);

			reststop.dispose();
			reststop = null;
		} else {
			// We quit the main application
			Gdx.app.exit();
		}
	}

	/** Set the cursor to the image specified by
	 * the path string
	 * @param path
	 */
	public void setCursor(String path){
		Pixmap pm = new Pixmap(Gdx.files.internal(path));
		Gdx.graphics.setCursor(Gdx.graphics.newCursor(pm,0 ,0 ));
		pm.dispose();
	}
	public void setFullScreen(boolean b){
		canvas.setFullscreen(b);
	}

}
