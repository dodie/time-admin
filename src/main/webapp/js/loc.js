var loc = (function(){
	var localizations = {};

	var getLocalizedValue = function(key) {
		if (localizations.hasOwnProperty(key)) {
			return localizations[key];
		} else {
			return key;
		}
	};

	var addLocalization = function(key, value) {
		if (!localizations.hasOwnProperty(key)) {
			localizations[key] = value;
		} else {
			throw new Error("Localization already set!");
		}
	}

	return {
		get: getLocalizedValue,
		add: addLocalization
	};
}());
