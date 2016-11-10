var exec = require('cordova/exec');

exports.shareScreen = function shareScreen(s, f) {
    exec(s, f, 'MkFacebook', 'shareScreen', []);
};

exports.shareLinkContent = function shareLinkContent(params, mode, s, f) {
    params = params || {};
    mode = mode || 'FEED';
    exec(s, f, 'MkFacebook', 'shareLinkContent', [params, mode]);
};
