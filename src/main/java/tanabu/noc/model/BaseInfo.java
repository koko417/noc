package tanabu.noc.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fasterxml.jackson.databind.JsonNode;

//共通ユーティリティ
public abstract class BaseInfo {
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Table {
		String name();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface Column {
		String name();

		boolean primaryKey() default false;
	}

	protected static String safeText(JsonNode node, int idx) {
		if (node.get(idx) == null || node.get(idx).isNull())
			return null;
		if (node.get(idx).isArray() && node.get(idx).size() > 0) {
			return node.get(idx).get(0).asText();
		}
		return node.get(idx).asText();
	}

	protected static int safeInt(JsonNode node, int idx) {
		return (node.get(idx) == null || node.get(idx).isNull()) ? -1 : node.get(idx).asInt();
	}

	protected static String extractContent(JsonNode node, int idx) {
		if (node.get(idx) == null || node.get(idx).isNull())
			return null;
		JsonNode block = node.get(idx);
		if (block.isArray() && block.size() >= 5) {
			JsonNode htmlBlock = block.get(4);
			if (htmlBlock != null && htmlBlock.isArray() && htmlBlock.size() > 1) {
				return htmlBlock.get(1).asText(); // <div>〜</div>
			}
		}
		return block.toString();
	}
	
}
