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

	private record Row(CookingStation header, RecipeHolder<CookingRecipe> recipe) {
	}

	/** A clickable ingredient in the detail view, and the recipe it links to. */
	private record Link(int x, int y, int width, RecipeHolder<CookingRecipe> target) {
	}

	private final List<RecipeHolder<CookingRecipe>> recipes = new ArrayList<>();
	private final List<Row> rows = new ArrayList<>();
	private final List<Link> links = new ArrayList<>();
	private final Deque<RecipeHolder<CookingRecipe>> history = new ArrayDeque<>();
	private RecipeHolder<CookingRecipe> selected;
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
		if (selected != null) extractDetail(graphics, mouseX, mouseY);
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
			if (row.header() != null) {
				graphics.text(font, stationName(row.header()).copy().withStyle(ChatFormatting.BOLD), x, rowY + 6, TEXT, false);
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
		if (recipe.station().heated()) facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.time", recipe.time() / 20));
		if (recipe.water() > 0) facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.water", recipe.water()));
		if (recipe.oil() > 0) facts.add(Component.translatable("screen.portable_pocket_cabin.cookbook.oil", recipe.oil()));
		if (!facts.isEmpty()) {
			Component line = facts.getFirst().copy();
			for (Component fact : facts.subList(1, facts.size())) line = line.copy().append(" · ").append(fact);
			graphics.text(font, line, x, y, MUTED, false);
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
			if (mouseX >= backX && mouseX < left + WIDTH - 8 && mouseY >= top + 8 && mouseY < top + 17) {
				selected = history.pop();
				return true;
			}
		}
		for (Link link : links) {
			if (mouseX >= link.x() && mouseX < link.x() + link.width() && mouseY >= link.y() && mouseY < link.y() + 9) {
				history.push(selected);
				selected = link.target();
				return true;
			}
		}
		int x = left + 8;
		if (mouseX >= x && mouseX < x + LIST_WIDTH - 8 && mouseY >= listTop() && mouseY < listTop() + visibleRows() * ROW) {
			int index = scroll + (int) ((mouseY - listTop()) / ROW);
			if (index < rows.size() && rows.get(index).recipe() != null) {
				history.clear();
				selected = rows.get(index).recipe();
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
