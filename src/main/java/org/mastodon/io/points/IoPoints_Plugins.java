package org.mastodon.io.points;

import static org.mastodon.app.ui.ViewMenuBuilder.item;
import static org.mastodon.app.ui.ViewMenuBuilder.menu;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.mastodon.app.ui.ViewMenuBuilder;
import org.mastodon.mamut.plugin.MamutPlugin;
import org.mastodon.mamut.plugin.MamutPluginAppModel;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.ui.keymap.CommandDescriptionProvider;
import org.mastodon.ui.keymap.CommandDescriptions;
import org.mastodon.ui.keymap.KeyConfigContexts;

import org.scijava.AbstractContextual;
import org.scijava.plugin.Plugin;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.ui.behaviour.util.AbstractNamedAction;
import org.scijava.ui.behaviour.util.Actions;
import org.scijava.ui.behaviour.util.RunnableAction;

@Plugin( type = MamutPlugin.class )
public class IoPoints_Plugins extends AbstractContextual implements MamutPlugin
{
	private static final String IMPORT_FROM_IMAGES = "[imports] import from instance segmentation";

	private static final String POINTS_EXPORT_3COLS = "[exports] export spots as 3col points";
	private static final String POINTS_IMPORT_3COLS = "[imports] import spots from 3col points";
	private static final String POINTS_EXPORT_4COLS = "[exports] export spots as 4col points";
	private static final String POINTS_IMPORT_4COLS = "[imports] import spots from 4col points";

	private static final String TRACKS_EXPORT_PLAINTXT = "[exports] export tracks as txt file";
	private static final String TRACKS_IMPORT_PLAINTXT = "[imports] import tracks from txt file";

	private static final String[] IMPORT_FROM_IMAGES_KEYS = { "not mapped" };

	private static final String[] POINTS_EXPORT_3COLS_KEYS = { "not mapped" };
	private static final String[] POINTS_IMPORT_3COLS_KEYS = { "not mapped" };
	private static final String[] POINTS_EXPORT_4COLS_KEYS = { "not mapped" };
	private static final String[] POINTS_IMPORT_4COLS_KEYS = { "not mapped" };

	private static final String[] TRACKS_EXPORT_PLAINTXT_KEYS = { "not mapped" };
	private static final String[] TRACKS_IMPORT_PLAINTXT_KEYS = { "not mapped" };
	//------------------------------------------------------------------------


	private static final Map< String, String > menuTexts = new HashMap<>();
	static
	{
		menuTexts.put( IMPORT_FROM_IMAGES, "Import from instance segmentation" );

		menuTexts.put( POINTS_EXPORT_3COLS, "Export to 3-column files" );
		menuTexts.put( POINTS_IMPORT_3COLS, "Import from 3-column file" );
		menuTexts.put( POINTS_EXPORT_4COLS, "Export to 4-column file" );
		menuTexts.put( POINTS_IMPORT_4COLS, "Import from 4-column file" );

		menuTexts.put( TRACKS_EXPORT_PLAINTXT, "Export to plain text file" );
		menuTexts.put( TRACKS_IMPORT_PLAINTXT, "Import from plain text file" );
	}
	@Override
	public Map< String, String > getMenuTexts() { return menuTexts; }

	@Override
	public List< ViewMenuBuilder.MenuItem > getMenuItems()
	{
		return Collections.singletonList( menu( "Plugins",
			menu( "Imports",
				item( POINTS_IMPORT_3COLS ),
				item( POINTS_IMPORT_4COLS ),
				item( TRACKS_IMPORT_PLAINTXT ),
				item( IMPORT_FROM_IMAGES )
			),
			menu( "Exports",
				item( POINTS_EXPORT_3COLS ),
				item( POINTS_EXPORT_4COLS ),
				item( TRACKS_EXPORT_PLAINTXT )
			)
		) );
	}

	/** Command descriptions for all provided commands */
	@Plugin( type = Descriptions.class )
	public static class Descriptions extends CommandDescriptionProvider
	{
		public Descriptions()
		{
			super( KeyConfigContexts.TRACKSCHEME, KeyConfigContexts.BIGDATAVIEWER );
		}

		@Override
		public void getCommandDescriptions( final CommandDescriptions descriptions )
		{
			descriptions.add(IMPORT_FROM_IMAGES,  IMPORT_FROM_IMAGES_KEYS,
					"Adds new spots from instance segmentation image series");
			descriptions.add(POINTS_EXPORT_3COLS, POINTS_EXPORT_3COLS_KEYS,
					"Exports spots x,y,z coordinates into text files in user-given folder, one file per one time point");
			descriptions.add(POINTS_IMPORT_3COLS, POINTS_IMPORT_3COLS_KEYS,
					"Adds new spots from a file with x,y,z coordinates, every spot creates own spatially-stationary track through-out the whole time-lapse");
			descriptions.add(POINTS_EXPORT_4COLS, POINTS_EXPORT_4COLS_KEYS,
					"Exports spots x,y,z,t coordinates into user-given text file");
			descriptions.add(POINTS_IMPORT_4COLS, POINTS_IMPORT_4COLS_KEYS,
					"Adds new spots from a file with x,y,z,t coordinates, no linking among the spots is done");

			descriptions.add(TRACKS_EXPORT_PLAINTXT, TRACKS_EXPORT_PLAINTXT_KEYS,
					"Exports spots and their linking information into user-given text file");
			descriptions.add(TRACKS_IMPORT_PLAINTXT, TRACKS_IMPORT_PLAINTXT_KEYS,
					"Imports spots and their linking information from user-given text file");
		}
	}
	//------------------------------------------------------------------------


	private final AbstractNamedAction importFromImagesAction;

	private final AbstractNamedAction exportThreeColumnPointsPerTimepointsAction;
	private final AbstractNamedAction importThreeColumnPointsAction;
	private final AbstractNamedAction exportFourColumnPointsAction;
	private final AbstractNamedAction importFourColumnPointsAction;
	private final AbstractNamedAction exportPlainTxtAction;
	private final AbstractNamedAction importPlainTxtAction;

	private MamutPluginAppModel pluginAppModel;

	public IoPoints_Plugins()
	{
		importFromImagesAction = new RunnableAction( IMPORT_FROM_IMAGES, this::importFromImages );

		exportThreeColumnPointsPerTimepointsAction = new RunnableAction( POINTS_EXPORT_3COLS, this::exportThreeColumnPointsPerTimepoints );
		importThreeColumnPointsAction              = new RunnableAction( POINTS_IMPORT_3COLS, this::importThreeColumnPoints );
		exportFourColumnPointsAction               = new RunnableAction( POINTS_EXPORT_4COLS, this::exportFourColumnPoints );
		importFourColumnPointsAction               = new RunnableAction( POINTS_IMPORT_4COLS, this::importFourColumnPoints );

		exportPlainTxtAction                       = new RunnableAction( TRACKS_EXPORT_PLAINTXT, this::exportTXT );
		importPlainTxtAction                       = new RunnableAction( TRACKS_IMPORT_PLAINTXT, this::importTXT );

		updateEnabledActions();
	}

	@Override
	public void setAppPluginModel( final MamutPluginAppModel model )
	{
		this.pluginAppModel = model;
		updateEnabledActions();
	}

	@Override
	public void installGlobalActions( final Actions actions )
	{
		actions.namedAction( importFromImagesAction, IMPORT_FROM_IMAGES_KEYS );

		actions.namedAction( exportThreeColumnPointsPerTimepointsAction, POINTS_EXPORT_3COLS_KEYS );
		actions.namedAction( importThreeColumnPointsAction,              POINTS_IMPORT_3COLS_KEYS );
		actions.namedAction( exportFourColumnPointsAction,               POINTS_EXPORT_4COLS_KEYS );
		actions.namedAction( importFourColumnPointsAction,               POINTS_IMPORT_4COLS_KEYS );
		actions.namedAction( exportPlainTxtAction,                       TRACKS_EXPORT_PLAINTXT_KEYS );
		actions.namedAction( importPlainTxtAction,                       TRACKS_IMPORT_PLAINTXT_KEYS );
	}

	private void updateEnabledActions()
	{
		final MamutAppModel appModel = ( pluginAppModel == null ) ? null : pluginAppModel.getAppModel();
		importFromImagesAction.setEnabled( appModel != null );

		exportThreeColumnPointsPerTimepointsAction.setEnabled( appModel != null );
		importThreeColumnPointsAction.setEnabled( appModel != null );
		exportFourColumnPointsAction.setEnabled( appModel != null );
		importFourColumnPointsAction.setEnabled( appModel != null );

		exportPlainTxtAction.setEnabled( appModel != null );
		importPlainTxtAction.setEnabled( appModel != null );
	}
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	private void importFromImages()
	{
		if ( pluginAppModel == null ) return;

		this.getContext().getService(CommandService.class).run(
			ReadInstanceSegmentationImages.class, true,
			"appModel", pluginAppModel.getAppModel(),
			"logService", this.getContext().getService(LogService.class));
	}

	private void exportThreeColumnPointsPerTimepoints()
	{
		this.getContext().getService(CommandService.class).run(
				WritePointsThreeColumnTXT.class, true,
				"appModel", pluginAppModel.getAppModel()
		);
	}
	private void importThreeColumnPoints()
	{
		this.getContext().getService(CommandService.class).run(
				ReadPointsTXT.class, true,
				"appModel", pluginAppModel.getAppModel(),
				"fourthColumnIsTime", false
		);
	}

	private void exportFourColumnPoints()
	{
		this.getContext().getService(CommandService.class).run(
				WritePointsFourColumnTXT.class, true,
				"appModel", pluginAppModel.getAppModel()
		);
	}
	private void importFourColumnPoints()
	{
		this.getContext().getService(CommandService.class).run(
				ReadPointsTXT.class, true,
				"appModel", pluginAppModel.getAppModel(),
				"fourthColumnIsTime", true
		);
	}

	private void exportTXT()
	{
		this.getContext().getService(CommandService.class).run(
				PlainTextFileExport.class, true,
				"pluginAppModel", pluginAppModel
		);
	}
	private void importTXT()
	{
		this.getContext().getService(CommandService.class).run(
				PlainTextFileImport.class, true,
				"pluginAppModel", pluginAppModel
		);
	}
}
