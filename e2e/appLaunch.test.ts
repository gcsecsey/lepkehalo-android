import { by, device, element, expect } from 'detox';

describe('App Launch', () => {
  beforeAll(async () => {
    await device.launchApp({ newInstance: true });
  });

  beforeEach(async () => {
    await device.reloadReactNative();
  });

  it('should show home screen on launch', async () => {
    await expect(element(by.id('home-screen'))).toBeVisible();
  });

  it('should show empty state on first launch', async () => {
    await expect(element(by.id('empty-state'))).toBeVisible();
  });

  it('should show scan button', async () => {
    await expect(element(by.id('scan-button'))).toBeVisible();
  });
});
