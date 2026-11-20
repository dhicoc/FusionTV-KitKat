        boolean removeAd = !Setting.isRemoveAd();
        Setting.putRemoveAd(removeAd);
        mBinding.removeAdText.setText(getSwitch(removeAd));
