var exec = require('cordova/exec');

exports.shareScreen = function shareScreen(s, f) {
    exec(s, f, 'MkFacebook', 'shareScreen', []);
};

exports.shareLinkDialog = function shareLinkDialog(params, mode, s, f) {
    params = params || {};
    mode = mode || 'FEED';
    exec(s, f, 'MkFacebook', 'shareLinkDialog', [params, mode]);
};
