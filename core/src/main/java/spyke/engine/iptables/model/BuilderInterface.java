package spyke.engine.iptables.model;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Value.Style(
        defaultAsDefault = true,
        get = {"get*", "is*", "should*"},
        depluralize = true,
        optionalAcceptNullable = true,
        implementationNestedInBuilder = true,
        visibility = Value.Style.ImplementationVisibility.PACKAGE,
        builderVisibility = Value.Style.BuilderVisibility.PUBLIC,
        defaults = @Value.Immutable(copy = false)
)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface BuilderInterface {
}
