module.exports = {
  '*.{js,jsx,ts,tsx}': ['eslint --fix', 'prettier --write'],
  '*.{json,md,yml,yaml}': ['prettier --write'],
  '*.java': ['./gradlew spotlessApply'],
  '*.tf': ['terraform fmt'],
};