package ca.landonjw.remoraids.implementation.commands.create.arguments;

import java.util.List;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

import ca.landonjw.remoraids.api.battles.IBattleRestraint;
import ca.landonjw.remoraids.api.battles.IBossBattleSettings;
import ca.landonjw.remoraids.api.commands.arguments.IRaidsArgument;
import ca.landonjw.remoraids.api.messages.placeholders.IParsingContext;
import ca.landonjw.remoraids.implementation.battles.restraints.SpeciesClauseRestraint;

public class SpeciesClauseRestraintArgument implements IRaidsArgument {

	@Override
	public List<String> getTokens() {
		return Lists.newArrayList("species-clause-restraint", "dup-spec");
	}

	@Override
	public void interpret(@Nonnull String value, @Nonnull IParsingContext context) throws IllegalArgumentException {
		if (context.getAssociation(IBossBattleSettings.class).isPresent()) {
			IBossBattleSettings settings = context.getAssociation(IBossBattleSettings.class).get();

			for (IBattleRestraint restraint : settings.getBattleRestraints()) {
				if (restraint instanceof SpeciesClauseRestraint)
					return;
			}

			settings.getBattleRestraints().add(new SpeciesClauseRestraint());
		}
	}
}