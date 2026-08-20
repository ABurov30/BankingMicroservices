.PHONY: dev dev-hot watch-compile build

dev:
	./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.config.import=optional:file:./.env.local[.properties]"

dev-hot:
	$(MAKE) -j2 dev watch-compile

watch-compile:
	command -v watchexec >/dev/null || (echo "watchexec is required. Install it with: brew install watchexec" && exit 1)
	watchexec -w src/main/java -w src/main/resources -- ./mvnw compile

build:
	./mvnw clean package
