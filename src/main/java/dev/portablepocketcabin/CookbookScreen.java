package dev.portablepocketcabin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** The cookbook: every cooking recipe by station, with what it needs, how it is made, and why. */
final class CookbookScreen extends Screen {
	private static final int WIDTH = 350;
	private static final int HEIGHT = 230;
	private static final int LIST_WIDTH = 136;
	private static final int ROW = 18;
	private static final int TEXT = 0xff404040;
	private static final int MUTED = 0xff808080;
	private static final int LINK = 0xff2050b0;
	private static final int GOOD = 0xff2a7a2a;
	/** The recipes the server last said the player can make. */
	static Set<Identifier> available = Set.of();

	/**
	 * A list row: a station header, a recipe, the tastes header (all null), one of the five tastes, the Garden header
	 * (an empty plant name), or a plant.
	 */
	/** Butchery rows and pages use the plant field, prefixed with this, so they select like Garden pages. */
	private static final String BUTCHERY = "butchery:";

	private record Row(CookingStation header, RecipeHolder<CookingRecipe> recipe, Seasoning.Taste taste, String plant) {
		Row(CookingStation header, RecipeHolder<CookingRecipe> recipe) {
			this(header, recipe, null, null);
		}

		Row(CookingStation header, RecipeHolder<CookingRecipe> recipe, Seasoning.Taste taste) {
			this(header, recipe, taste, null);
		}

		boolean tastesHeader() {
			return header == null && recipe == null && taste == null && plant == null;
		}

		boolean gardenHeader() {
			return plant != null && plant.isEmpty();
		}

		boolean butcheryHeader() {
			return BUTCHERY.equals(plant);
		}

		/** A Garden plant or a Butchery animal: a row that opens its own page. */
		boolean plantRow() {
			return plant != null && !plant.isEmpty() && !plant.equals(BUTCHERY);
		}
	}

	/** A clickable ingredient in the detail view, and the recipe it links to. */
	private record Link(int x, int y, int width, RecipeHolder<CookingRecipe> target) {
	}

	private final List<RecipeHolder<CookingRecipe>> recipes = new ArrayList<>();
	private final List<Row> rows = new ArrayList<>();
	private final List<Link> links = new ArrayList<>();
	private final Deque<RecipeHolder<CookingRecipe>> history = new ArrayDeque<>();
	private RecipeHolder<CookingRecipe> selected;
	/** The taste page shown instead of a recipe, or null. */
	private Seasoning.Taste selectedTaste;
	/** The Garden page shown instead of a recipe, or null. */
	private String selectedPlant;
	private EditBox search;
	private int scroll;
	private int left;
	private int top;

	CookbookScreen() {
		super(Component.translatable("item.portable_pocket_cabin.cookbook"));
	}

	/** Opens the book and asks the server which recipes the player can make. */
	static void open() {
		var minecraft = net.minecraft.client.Minecraft.getInstance();
		minecraft.gui.setScreen(new CookbookScreen());
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new CookbookAvailability.Request(false));
	}

	@Override
	protected void init() {
		left = (width - WIDTH) / 2;
		top = (height - HEIGHT) / 2;
		recipes.clear();
		if (minecraft.level != null) {
			recipes.addAll(CookbookAvailability.recipes(minecraft.level.recipeAccess().getSynchronizedRecipes().recipes()));
		}
		recipes.sort(Comparator.<RecipeHolder<CookingRecipe>>comparingInt(holder -> holder.value().station().ordinal())
			.thenComparing(holder -> name(holder).getString()));
		String query = search == null ? "" : search.getValue();
		search = addRenderableWidget(new EditBox(font, left + 8, top + 8, LIST_WIDTH - 8, 14,
			Component.translatable("screen.portable_pocket_cabin.cookbook.search")));
		search.setHint(Component.translatable("screen.portable_pocket_cabin.cookbook.search").withStyle(ChatFormatting.GRAY));
		search.setValue(query);
		search.setResponder(value -> {
			scroll = 0;
			rebuild();
		});
		rebuild();
		if (selected == null && !recipes.isEmpty()) selected = recipes.getFirst();
	}

	private void rebuild() {
		rows.clear();
		String query = search.getValue().trim().toLowerCase(Locale.ROOT);
		List<Row> tastes = new ArrayList<>();
		for (Seasoning.Taste taste : Seasoning.Taste.values()) {
			if (query.isEmpty() || tasteName(taste).getString().toLowerCase(Locale.ROOT).contains(query)) tastes.add(new Row(null, null, taste));
		}
		if (!tastes.isEmpty()) {
			rows.add(new Row(null, null, null));
			rows.addAll(tastes);
		}
		List<Row> garden = new ArrayList<>();
		for (String plant : CookingPlants.PLANTS.keySet()) {
			if (query.isEmpty() || plantName(plant).getString().toLowerCase(Locale.ROOT).contains(query)) garden.add(new Row(null, null, null, plant));
		}
		garden.sort(Comparator.comparing(row -> plantName(row.plant()).getString()));
		if (!garden.isEmpty()) {
			rows.add(new Row(null, null, null, ""));
			rows.addAll(garden);
		}
		List<Row> butchery = new ArrayList<>();
		for (Identifier animal : butcheryOrder()) {
			if (query.isEmpty() || carcassName(animal).getString().toLowerCase(Locale.ROOT).contains(query)) {
				butchery.add(new Row(null, null, null, BUTCHERY + animal));
			}
		}
		if (!butchery.isEmpty()) {
			rows.add(new Row(null, null, null, BUTCHERY));
			rows.addAll(butchery);
		}
		CookingStation station = null;
		for (RecipeHolder<CookingRecipe> holder : recipes) {
			if (!query.isEmpty() && !matches(holder.value(), query) && !name(holder).getString().toLowerCase(Locale.ROOT).contains(query)) continue;
			if (holder.value().station() != station) {
				station = holder.value().station();
				rows.add(new Row(station, null));
			}
			rows.add(new Row(null, holder));
		}
	}

	private static boolean matches(CookingRecipe recipe, String query) {
		if (stationName(recipe.station()).getString().toLowerCase(Locale.ROOT).contains(query)) return true;
		if (techniqueName(recipe.technique()).getString().toLowerCase(Locale.ROOT).contains(query)) return true;
		return recipe.ingredients().stream().anyMatch(entry -> icon(entry.ingredient()).getHoverName().getString()
			.toLowerCase(Locale.ROOT).contains(query));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		CookingScreen.panel(graphics, left, top, WIDTH, HEIGHT);
		graphics.fill(left + LIST_WIDTH + 4, top + 8, left + LIST_WIDTH + 5, top + HEIGHT - 8, 0xffa0a0a0);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		extractList(graphics, mouseX, mouseY);
		links.clear();
		if (selectedPlant != null && selectedPlant.startsWith(BUTCHERY)) extractCarcass(graphics, mouseX, mouseY);
		else if (selectedPlant != null) extractPlant(graphics, mouseX, mouseY);
		else if (selectedTaste != null) extractTaste(graphics, mouseX, mouseY);
		else if (selected != null) extractDetail(graphics, mouseX, mouseY);
	}

	private int listTop() {
		return top + 26;
	}

	private int visibleRows() {
		return (HEIGHT - 34) / ROW;
	}

	private void extractList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = left + 8;
		int y = listTop();
		graphics.enableScissor(x, y, x + LIST_WIDTH - 8, y + visibleRows() * ROW);
		for (int index = scroll; index < rows.size() && index < scroll + visibleRows(); index++) {
			Row row = rows.get(index);
			int rowY = y + (index - scroll) * ROW;
			if (row.header() != null || row.tastesHeader() || row.gardenHeader() || row.butcheryHeader()) {
				Component header = row.header() != null ? stationName(row.header()) : Component.translatable(row.gardenHeader()
					? "screen.portable_pocket_cabin.cookbook.garden_header" : row.butcheryHeader()
					? "screen.portable_pocket_cabin.cookbook.butchery_header" : "screen.portable_pocket_cabin.cookbook.tastes_header");
				graphics.text(font, header.copy().withStyle(ChatFormatting.BOLD), x, rowY + 6, TEXT, false);
				continue;
			}
			if (row.plantRow()) {
				boolean hoveredPlant = mouseX >= x && mouseX < x + LIST_WIDTH - 8 && mouseY >= rowY && mouseY < rowY + ROW;
				if (row.plant().equals(selectedPlant)) graphics.fill(x, rowY, x + LIST_WIDTH - 8, rowY + ROW, 0x40000000);
				else if (hoveredPlant) graphics.fill(x, rowY, x + LIST_WIDTH - 8, rowY + ROW, 0x20000000);
				boolean animal = row.plant().startsWith(BUTCHERY);
				Identifier carcass = animal ? Identifier.parse(row.plant().substring(BUTCHERY.length())) : null;
				graphics.item(animal ? new ItemStack(CarcassData.client.get(carcass).carcass()) : new ItemStack(CookingPlants.PLANTED_FROM.get(row.plant())),
					x + 1, rowY + 1);
				Component name = animal ? carcassName(carcass) : plantName(row.plant());
				graphics.text(font, fit(name.getString(), LIST_WIDTH - 32), x + 20, rowY + 5, TEXT, false);
				continue;
			}
			if (row.taste() != null) {
				boolean hoveredTaste = mouseX >= x && mouseX < x + LIST_WIDTH - 8 && mouseY >= rowY && mouseY < rowY + ROW;
				if (row.taste() == selectedTaste) graphics.fill(x, rowY, x + LIST_WIDTH - 8, rowY + ROW, 0x40000000);
				else if (hoveredTaste) graphics.fill(x, rowY, x + LIST_WIDTH - 8, rowY + ROW, 0x20000000);
				graphics.text(font, tasteName(row.taste()), x + 20, rowY + 5, TEXT, false);
				continue;
			}
			boolean hovered = mouseX >= x && mouseX < x + LIST_WIDTH - 8 && mouseY >= rowY && mouseY < rowY + ROW;
			if (row.recipe() == selected) graphics.fill(x, rowY, x + LIST_WIDTH - 8, rowY + ROW, 0x40000000);
			else if (hovered) graphics.fill(x, rowY, x + LIST_WIDTH - 8, rowY + ROW, 0x20000000);
			graphics.item(row.recipe().value().result().create(), x + 1, rowY + 1);
			boolean canMake = available.contains(row.recipe().id().identifier());
			graphics.text(font, fit(name(row.recipe()).getString(), LIST_WIDTH - 32), x + 20, rowY + 5, canMake ? GOOD : TEXT, false);
		}
		graphics.disableScissor();
		if (rows.size() > visibleRows()) {
			int trackHeight = visibleRows() * ROW;
			int thumb = Math.max(10, trackHeight * visibleRows() / rows.size());
			int offset = (trackHeight - thumb) * scroll / Math.max(1, rows.size() - visibleRows());
			graphics.fill(left + LIST_WIDTH, y + offset, left + LIST_WIDTH + 2, y + offset + thumb, 0xff808080);
		}
	}

	private void extractDetail(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		CookingRecipe recipe = selected.value();
		int x = left + LIST_WIDTH + 12;
		int right = left + WIDTH - 8;
		int y = top + 8;
		if (!history.isEmpty()) {
			Component back = Component.translatable("screen.portable_pocket_cabin.cookbook.back");
			graphics.text(font, back, right - font.width(back), y, LINK, false);
		}
		ItemStack result = recipe.result().create();
		graphics.item(result, x, y);
		hoverItem(graphics, result, x, y, mouseX, mouseY);
		Component title = result.getCount() > 1 ? Component.literal(result.getCount() + " × ").append(result.getHoverName()) : result.getHoverName();
		graphics.text(font, title, x + 20, y + 4, TEXT, false);
		y += 20;
		graphics.text(font, stationName(recipe.station()).copy().append(" · ").append(techniqueName(recipe.technique())), x, y, MUTED, false);
		y += 11;
		List<Component> facts = new ArrayList<>();
		recipe.heat().ifPresent(heat -> facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.heat",
			Component.translatable("screen.portable_pocket_cabin.cooking.heat." + heat.getSerializedName()))));
		if (recipe.station().worked()) {
			facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.uses", recipe.time()));
		} else if (recipe.station().timeStation()) {
			int hours = Math.max(1, recipe.time() / CookingStation.HOUR);
			facts.add(hours >= 24 && hours % 24 == 0 ? Component.translatable("screen.portable_pocket_cabin.cookbook.days", hours / 24)
				: Component.translatable("screen.portable_pocket_cabin.cookbook.hours", hours));
		} else if (recipe.station().timed()) {
			facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.time", Math.max(1, recipe.time() / 20)));
		}
		recipe.sun().ifPresent(sun -> facts.add(Component.translatable(sun ? "screen.portable_pocket_cabin.cookbook.sun"
			: "screen.portable_pocket_cabin.cookbook.shade")));
		if (recipe.water() > 0) facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.water", recipe.water()));
		if (recipe.oil() > 0) facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.oil", recipe.oil()));
		if (!facts.isEmpty()) {
			Component line = facts.getFirst().copy();
			for (Component fact : facts.subList(1, facts.size())) line = line.copy().append(" · ").append(fact);
			graphics.text(font, line, x, y, MUTED, false);
			y += 13;
		}
		Component tastes = tasteLine(Seasoning.tastes(result), recipe.tastes());
		if (tastes != null) {
			graphics.text(font, tastes, x, y, MUTED, false);
			y += 13;
		}
		graphics.text(font, Component.translatable("screen.portable_pocket_cabin.cookbook.ingredients"), x, y, TEXT, false);
		y += 11;
		for (CookingRecipe.Counted entry : recipe.ingredients()) y = ingredient(graphics, entry, x, y, mouseX, mouseY);
		if (recipe.container().isPresent()) {
			graphics.text(font, Component.translatable("screen.portable_pocket_cabin.cookbook.container"), x, y + 2, TEXT, false);
			y += 11;
			y = ingredient(graphics, recipe.container().get(), x, y, mouseX, mouseY);
		}
		if (!recipe.remainders().isEmpty()) {
			Component also = Component.translatable("screen.portable_pocket_cabin.cookbook.byproducts");
			StringBuilder names = new StringBuilder();
			for (ItemStackTemplate remainder : recipe.remainders()) {
				ItemStack stack = remainder.create();
				if (!names.isEmpty()) names.append(", ");
				names.append(stack.getCount()).append(" × ").append(stack.getHoverName().getString());
			}
			graphics.text(font, also.copy().append(" ").append(names.toString()), x, y + 2, MUTED, false);
			y += 13;
		}
		boolean canMake = available.contains(selected.id().identifier());
		graphics.text(font, Component.translatable(canMake ? "screen.portable_pocket_cabin.cookbook.have" : "screen.portable_pocket_cabin.cookbook.missing"),
			x, y + 2, canMake ? GOOD : MUTED, false);
		y += 14;
		Component description = techniqueDescription(recipe.technique());
		graphics.textWithWordWrap(font, description, x, y, right - x, TEXT, false);
		// A product can add its own note, such as which oil is for frying and which is for dressing.
		String note = result.getItem().getDescriptionId() + ".cookbook_note";
		if (Language.getInstance().has(note)) {
			y += font.split(description, right - x).size() * font.lineHeight + 4;
			graphics.textWithWordWrap(font, Component.translatable(note), x, y, right - x, MUTED, false);
		}
	}

	/** The tastes a result has and the tastes its recipe wants for seasoning, or null when it has neither. */
	private static Component tasteLine(java.util.Set<Seasoning.Taste> has, List<Seasoning.Taste> wants) {
		if (has.isEmpty() && wants.isEmpty()) return null;
		Component line = Component.empty();
		if (!has.isEmpty()) line = line.copy().append(Component.translatable("screen.portable_pocket_cabin.cookbook.has_tastes", tasteList(has)));
		if (!wants.isEmpty()) {
			if (!has.isEmpty()) line = line.copy().append(" · ");
			line = line.copy().append(Component.translatable("screen.portable_pocket_cabin.cookbook.wants_tastes", tasteList(wants)));
		}
		return line;
	}

	private static Component tasteList(java.util.Collection<Seasoning.Taste> tastes) {
		Component list = Component.empty();
		boolean first = true;
		for (Seasoning.Taste taste : tastes) {
			if (!first) list = list.copy().append(", ");
			list = list.copy().append(tasteName(taste));
			first = false;
		}
		return list;
	}

	static Component tasteName(Seasoning.Taste taste) {
		return Component.translatable("taste.portable_pocket_cabin." + taste.getSerializedName());
	}

	/** A taste's page: what the taste is, and the ingredients and dishes in the mod that carry it. */
	private void extractTaste(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = left + LIST_WIDTH + 12;
		int right = left + WIDTH - 8;
		int y = top + 8;
		graphics.text(font, tasteName(selectedTaste).copy().withStyle(ChatFormatting.BOLD), x, y + 4, TEXT, false);
		y += 20;
		Component description = Component.translatable("taste.portable_pocket_cabin." + selectedTaste.getSerializedName() + ".description");
		graphics.textWithWordWrap(font, description, x, y, right - x, TEXT, false);
		y += font.split(description, right - x).size() * font.lineHeight + 8;
		graphics.text(font, Component.translatable("screen.portable_pocket_cabin.cookbook.sources"), x, y, TEXT, false);
		y += 12;
		int column = x;
		for (var holder : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(selectedTaste.tag())) {
			if (y > top + HEIGHT - 24) break;
			ItemStack stack = new ItemStack(holder);
			graphics.item(stack, column, y);
			hoverItem(graphics, stack, column, y, mouseX, mouseY);
			column += 18;
			if (column + 16 > right) {
				column = x;
				y += 18;
			}
		}
	}

	/** A plant's Garden name: its own title when the lang has one, such as "Plum Tree", otherwise its block's name. */
	static Component plantName(String plant) {
		String key = "garden.portable_pocket_cabin." + plant + ".name";
		return Language.getInstance().has(key) ? Component.translatable(key) : CookingPlants.PLANTS.get(plant).getName();
	}

	/** A plant's Garden page: where it grows wild, what it needs, what it is planted from, and what it gives. */
	private void extractPlant(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		int x = left + LIST_WIDTH + 12;
		int right = left + WIDTH - 8;
		int y = top + 8;
		ItemStack planted = new ItemStack(CookingPlants.PLANTED_FROM.get(selectedPlant));
		graphics.item(planted, x, y);
		hoverItem(graphics, planted, x, y, mouseX, mouseY);
		graphics.text(font, plantName(selectedPlant).copy().withStyle(ChatFormatting.BOLD), x + 20, y + 4, TEXT, false);
		y += 22;
		Component wild = Component.translatable("screen.portable_pocket_cabin.cookbook.grows_wild", wildBiomes(selectedPlant));
		graphics.textWithWordWrap(font, wild, x, y, right - x, TEXT, false);
		y += font.split(wild, right - x).size() * font.lineHeight + 6;
		var block = CookingPlants.PLANTS.get(selectedPlant);
		var needs = PlantNeeds.client.getOrDefault(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block), PlantNeeds.DEFAULT);
		Component growing = Component.translatable("screen.portable_pocket_cabin.cookbook.growing", soilNames(needs), needs.light(),
			Component.translatable("screen.portable_pocket_cabin.cookbook.water." + needs.water().getSerializedName()));
		if (needs.support() != PlantNeeds.Support.NONE) {
			growing = growing.copy().append(" ").append(Component.translatable("screen.portable_pocket_cabin.cookbook.support." + needs.support().getSerializedName()));
		}
		if (needs.shade()) growing = growing.copy().append(" ").append(Component.translatable("screen.portable_pocket_cabin.cookbook.shade_needed"));
		graphics.textWithWordWrap(font, growing, x, y, right - x, TEXT, false);
		y += font.split(growing, right - x).size() * font.lineHeight + 6;
		graphics.text(font, Component.translatable("screen.portable_pocket_cabin.cookbook.gives"), x, y, TEXT, false);
		y += 11;
		for (Item item : CookingPlants.GIVES.getOrDefault(selectedPlant, List.of())) {
			ItemStack stack = new ItemStack(item);
			graphics.item(stack, x, y);
			hoverItem(graphics, stack, x, y, mouseX, mouseY);
			graphics.text(font, stack.getHoverName(), x + 20, y + 4, TEXT, false);
			y += 16;
		}
		String key = "garden.portable_pocket_cabin." + selectedPlant;
		if (Language.getInstance().has(key)) graphics.textWithWordWrap(font, Component.translatable(key), x, y + 4, right - x, MUTED, false);
	}

	/** The biomes in a plant's grows_wild tag, as names, from the client's synced registries. */
	private Component wildBiomes(String plant) {
		if (minecraft.level == null) return Component.empty();
		var tag = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BIOME,
			PortablePocketCabin.id("grows_wild/" + CookingPlants.wildName(plant)));
		var biomes = minecraft.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME).get(tag);
		if (biomes.isEmpty()) return Component.translatable("screen.portable_pocket_cabin.cookbook.nowhere");
		List<String> names = new ArrayList<>();
		for (var biome : biomes.get()) {
			biome.unwrapKey().ifPresent(key -> names.add(Component.translatable("biome." + key.identifier().getNamespace() + "." + key.identifier().getPath()).getString()));
		}
		names.sort(String::compareTo);
		return Component.literal(String.join(", ", names));
	}

	private static Component soilNames(PlantNeeds needs) {
		List<String> soils = new ArrayList<>();
		for (String soil : needs.soil()) {
			if (soil.startsWith("#")) {
				soils.add(Component.translatable("screen.portable_pocket_cabin.cookbook.soil." + soil.substring(1).replace(':', '.')).getString());
			} else {
				var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Identifier.parse(soil));
				soils.add(block.getName().getString());
			}
		}
		return Component.literal(String.join(", ", soils));
	}

	/** The animals and fish with carcass data, in the order the butchery reference lists them, then any datapack adds. */
	private static List<Identifier> butcheryOrder() {
		List<Identifier> order = new ArrayList<>(CarcassData.client.keySet());
		List<String> known = List.of(Butchery.ANIMALS);
		order.sort(Comparator.<Identifier>comparingInt(id -> known.contains(id.getPath()) ? known.indexOf(id.getPath()) : known.size())
			.thenComparing(Identifier::toString));
		return order;
	}

	/** An animal's or fish's Butchery name, from the lang when it has one, otherwise its carcass item's name. */
	private static Component carcassName(Identifier animal) {
		String key = "butchery." + animal.getNamespace() + "." + animal.getPath();
		if (Language.getInstance().has(key)) return Component.translatable(key);
		CarcassData data = CarcassData.client.get(animal);
		return data == null ? Component.literal(animal.toString()) : new ItemStack(data.carcass()).getHoverName();
	}

	/** An animal's Butchery page: how it is dressed, what dressing gives, and every part in the order the block takes them. */
	private void extractCarcass(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Identifier animal = Identifier.parse(selectedPlant.substring(BUTCHERY.length()));
		CarcassData data = CarcassData.client.get(animal);
		if (data == null) return;
		int x = left + LIST_WIDTH + 12;
		int right = left + WIDTH - 8;
		int y = top + 8;
		ItemStack carcass = new ItemStack(data.carcass());
		graphics.item(carcass, x, y);
		hoverItem(graphics, carcass, x, y, mouseX, mouseY);
		graphics.text(font, carcassName(animal).copy().withStyle(ChatFormatting.BOLD), x + 20, y + 4, TEXT, false);
		y += 22;
		Component dressing = Component.translatable("screen.portable_pocket_cabin.cookbook.dressing." + data.dressing().getSerializedName());
		if (data.scald()) dressing = dressing.copy().append(" ").append(Component.translatable("screen.portable_pocket_cabin.cookbook.scald_first"));
		graphics.textWithWordWrap(font, dressing, x, y, right - x, TEXT, false);
		y += font.split(dressing, right - x).size() * font.lineHeight + 4;
		boolean fish = data.dressing() == CarcassData.Dressing.NONE;
		y = itemRow(graphics, data.dressingGives(), fish ? "screen.portable_pocket_cabin.cookbook.knife_first_gives"
			: "screen.portable_pocket_cabin.cookbook.dressing_gives", x, y, right, mouseX, mouseY);
		y = itemRow(graphics, data.parts(), fish ? "screen.portable_pocket_cabin.cookbook.parts_in_order_knife"
			: "screen.portable_pocket_cabin.cookbook.parts_in_order", x, y, right, mouseX, mouseY);
		String note = "butchery." + animal.getNamespace() + "." + animal.getPath() + ".note";
		if (Language.getInstance().has(note)) graphics.textWithWordWrap(font, Component.translatable(note), x, y + 2, right - x, MUTED, false);
	}

	/** A heading and a wrapped row of item icons with their counts, hovering for names. Returns the y below it. */
	private int itemRow(GuiGraphicsExtractor graphics, List<net.minecraft.world.item.ItemStackTemplate> items, String heading, int x, int y, int right,
		int mouseX, int mouseY) {
		if (items.isEmpty()) return y;
		graphics.text(font, Component.translatable(heading), x, y, TEXT, false);
		y += 11;
		int column = x;
		for (var template : items) {
			if (column + 18 > right) {
				column = x;
				y += 18;
			}
			ItemStack stack = template.create();
			graphics.item(stack, column, y);
			if (stack.getCount() > 1) {
				String count = String.valueOf(stack.getCount());
				graphics.text(font, count, column + 17 - font.width(count), y + 9, 0xffffffff, true);
			}
			hoverItem(graphics, stack, column, y, mouseX, mouseY);
			column += 18;
		}
		return y + 22;
	}

	/** Shows an animal's Butchery page, as the cookbook client GameTest does. */
	void selectCarcass(String animal) {
		selectedPlant = BUTCHERY + PortablePocketCabin.id(animal);
		selectedTaste = null;
	}

	/** Shows a plant's Garden page, as the cookbook client GameTest does. */
	void selectPlant(String plant) {
		selectedPlant = plant;
		selectedTaste = null;
	}

	/** Shows a taste's page, as the cookbook client GameTest does. */
	void selectTaste(Seasoning.Taste taste) {
		selectedTaste = taste;
		selectedPlant = null;
	}

	/** Draws one ingredient line. An ingredient that another recipe makes is a link to that recipe. */
	private int ingredient(GuiGraphicsExtractor graphics, CookingRecipe.Counted entry, int x, int y, int mouseX, int mouseY) {
		ItemStack icon = icon(entry.ingredient());
		graphics.item(icon, x, y);
		hoverItem(graphics, icon, x, y, mouseX, mouseY);
		Component label = Component.literal(entry.count() + " × ").append(icon.getHoverName());
		Optional<RecipeHolder<CookingRecipe>> source = recipes.stream()
			.filter(holder -> entry.ingredient().test(holder.value().result().create())).findFirst();
		int labelWidth = font.width(label);
		boolean hovered = source.isPresent() && mouseX >= x + 20 && mouseX < x + 20 + labelWidth && mouseY >= y + 4 && mouseY < y + 13;
		graphics.text(font, source.isPresent() ? label.copy().withStyle(hovered ? ChatFormatting.UNDERLINE : ChatFormatting.RESET) : label,
			x + 20, y + 4, source.isPresent() ? LINK : TEXT, false);
		source.ifPresent(target -> links.add(new Link(x + 20, y + 4, labelWidth, target)));
		return y + 16;
	}

	/** The text, shortened with an ellipsis when it is wider than {@code width}. */
	private String fit(String text, int width) {
		if (font.width(text) <= width) return text;
		return font.plainSubstrByWidth(text, width - font.width("…")).stripTrailing() + "…";
	}

	private void hoverItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
		if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
	}

	/** An item that the ingredient accepts. Tags cycle through their items once a second. */
	static ItemStack icon(Ingredient ingredient) {
		List<Holder<Item>> items = ingredient.items().toList();
		if (items.isEmpty()) return ItemStack.EMPTY;
		return new ItemStack(items.get((int) (System.currentTimeMillis() / 1000 % items.size())));
	}

	private static Component name(RecipeHolder<CookingRecipe> holder) {
		return holder.value().result().create().getHoverName();
	}

	static Component stationName(CookingStation station) {
		return Component.translatable("screen.portable_pocket_cabin.cookbook.station." + station.path());
	}

	private static Component techniqueName(String technique) {
		return Component.translatable("technique.portable_pocket_cabin." + technique);
	}

	private static Component techniqueDescription(String technique) {
		return Component.translatable("technique.portable_pocket_cabin." + technique + ".description");
	}

	/** Selects a recipe, as the cookbook client GameTest does. */
	void select(Identifier id) {
		recipes.stream().filter(holder -> holder.id().identifier().equals(id)).findFirst().ifPresent(holder -> {
			if (selected != null && selected != holder) history.push(selected);
			selected = holder;
			selectedTaste = null;
			selectedPlant = null;
		});
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) return true;
		double mouseX = event.x();
		double mouseY = event.y();
		if (!history.isEmpty()) {
			Component back = Component.translatable("screen.portable_pocket_cabin.cookbook.back");
			int backX = left + WIDTH - 8 - font.width(back);
			if (selectedTaste == null && selectedPlant == null && mouseX >= backX && mouseX < left + WIDTH - 8 && mouseY >= top + 8 && mouseY < top + 17) {
				selected = history.pop();
				return true;
			}
		}
		for (Link link : links) {
			if (selectedTaste != null || selectedPlant != null) break;
			if (mouseX >= link.x() && mouseX < link.x() + link.width() && mouseY >= link.y() && mouseY < link.y() + 9) {
				history.push(selected);
				selected = link.target();
				return true;
			}
		}
		int x = left + 8;
		if (mouseX >= x && mouseX < x + LIST_WIDTH - 8 && mouseY >= listTop() && mouseY < listTop() + visibleRows() * ROW) {
			int index = scroll + (int) ((mouseY - listTop()) / ROW);
			if (index < rows.size() && rows.get(index).plantRow()) {
				selectPlant(rows.get(index).plant());
				return true;
			}
			if (index < rows.size() && rows.get(index).taste() != null) {
				selectTaste(rows.get(index).taste());
				return true;
			}
			if (index < rows.size() && rows.get(index).recipe() != null) {
				history.clear();
				selected = rows.get(index).recipe();
				selectedTaste = null;
				selectedPlant = null;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (mouseX < left + LIST_WIDTH + 4) {
			scroll = Math.max(0, Math.min(rows.size() - visibleRows(), scroll - (int) Math.signum(vertical)));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}
}
